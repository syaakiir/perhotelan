package com.pnj.hotel

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.pnj.hotel.auth.SettingsActivity
import com.pnj.hotel.chat.ChatActivity
import com.pnj.hotel.databinding.ActivityMainBinding
import com.pnj.hotel.tamu.AddTamuActivity
import com.pnj.hotel.tamu.Tamu
import com.pnj.hotel.tamu.TamuAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var tamuRecyclerView: RecyclerView
    private lateinit var tamuArrayList: ArrayList<Tamu>
    private lateinit var tamuAdapter: TamuAdapter
    private lateinit var db : FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tamuRecyclerView = binding.tamuListView
        tamuRecyclerView.layoutManager = LinearLayoutManager(this)
        tamuRecyclerView.setHasFixedSize(true)

        tamuArrayList = arrayListOf()
        tamuAdapter = TamuAdapter(tamuArrayList)

        tamuRecyclerView.adapter = tamuAdapter

        load_data()

        binding.btnAddTamu.setOnClickListener{
            val intentMain = Intent(this,AddTamuActivity::class.java)
            startActivity(intentMain)
        }

        binding.txtSearchTamu.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                val keyword = binding.txtSearchTamu.text.toString()
                if (keyword.isNotEmpty()){
                    search_data(keyword)
                }
                else{
                    load_data()
                }
            }

            override fun afterTextChanged(p0: Editable?) {}
        })

        swipeDelete()

        binding.bottomNavigation.setOnItemSelectedListener {
            when(it.itemId){
                R.id.nav_bottom_home -> {
                    val intent= Intent(this,MainActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_bottom_setting -> {
                    val intent= Intent(this,SettingsActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_bottom_chat -> {
                    val intent= Intent(this,ChatActivity::class.java)
                    startActivity(intent)
                }
            }
            true
        }
    }

    private fun load_data(){
        tamuArrayList.clear()
        db = FirebaseFirestore.getInstance()
        db.collection("tamu").
        addSnapshotListener(object : EventListener<QuerySnapshot>{
            override fun onEvent(
                value: QuerySnapshot?,
                error: FirebaseFirestoreException?
            ) {
                if (error != null){
                    Log.e("Firestore Error",error.message.toString())
                    return
                }
                for (dc: DocumentChange in value?.documentChanges!!){
                    if(dc.type == DocumentChange.Type.ADDED)
                        tamuArrayList.add(dc.document.toObject(Tamu::class.java))
                }
                tamuAdapter.notifyDataSetChanged()
            }
        })
    }

//    private fun search_data(keyword : String){
//        tamuArrayList.clear()
//
//        db = FirebaseFirestore.getInstance()
//
//        val query = db.collection("tamu")
//            .orderBy("nama")
//            .startAt(keyword)
//            .get()
//        query.addOnSuccessListener {
//            tamuArrayList.clear()
//            for (document in it) {
//                tamuArrayList.add(document.toObject(Tamu::class.java))
//            }
//        }
//    }

    private fun search_data(keyword: String) {
        tamuArrayList.clear()

        // Use the same Firestore instance as in the load_data() method
        val query = db.collection("tamu")
            .orderBy("nama")
            .startAt(keyword)
            .endAt(keyword + "\uf8ff") // This is to allow partial text matching
            .get()

        query.addOnSuccessListener {
            tamuArrayList.clear()
            for (document in it) {
                tamuArrayList.add(document.toObject(Tamu::class.java))
            }
            tamuAdapter.notifyDataSetChanged()
        }
        query.addOnFailureListener { e ->
            Log.e("Firestore Error", e.message.toString())
            // Show a toast or handle the error as you prefer
        }
    }


    fun deleteTamu(tamu: Tamu,doc_id:String){
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Apakah ${tamu.nama} ingin dihapus ?")
            .setCancelable(false)
            .setPositiveButton("Yes"){dialog,id ->
                lifecycleScope.launch{
                    db.collection("tamu")
                        .document(doc_id).delete()
                    deleteFoto("img_tamu/${tamu.nik}_${tamu.nama}.jpg")
                    Toast.makeText(
                        applicationContext,
                        tamu.nama.toString() + " is deleted",
                        Toast.LENGTH_LONG
                    ).show()
                    load_data()
                }
            }
            .setNegativeButton("No"){dialog,id ->
                dialog.dismiss()
                load_data()
            }
        val alert = builder.create()
        alert.show()
    }

    fun swipeDelete(){
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0,
            ItemTouchHelper.RIGHT){
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition

                lifecycleScope.launch{
                    val tamu = tamuArrayList[position]
                    val personQuery = db.collection("tamu")
                        .whereEqualTo("nik",tamu.nik)
                        .whereEqualTo("nama",tamu.nama)
                        .whereEqualTo("jenis_kelamin",tamu.jenis_kelamin)
                        .whereEqualTo("tgl_lahir",tamu.tgl_lahir)
                        .get()
                        .await()
                    if (personQuery.documents.isNotEmpty()){
                        for (document in personQuery){
                            try {
                                deleteTamu(tamu,document.id)
                                load_data()
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main){
                                    Toast.makeText(
                                        applicationContext,
                                        e.message.toString(),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                    else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                applicationContext,
                                "User yang ingin di hapus tidak ditemukan",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }).attachToRecyclerView(tamuRecyclerView)
    }

    private fun deleteFoto(file_name: String){
        val storage = Firebase.storage
        val storageRef = storage.reference
        val deleteFileRef = storageRef.child(file_name)
        if (deleteFileRef != null){
            deleteFileRef.delete().addOnSuccessListener {
                Log.e("deleted","success")
            }.addOnFailureListener{
                Log.e("deleted","failed")
            }
        }
    }
}

