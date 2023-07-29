package com.pnj.hotel.auth

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.pnj.hotel.databinding.ActivitySignUpBinding

class SignUpActivity : AppCompatActivity(){
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseAuth = FirebaseAuth.getInstance()

        binding.btnSignup.setOnClickListener {
            val email = binding.txtSignupEmail.text.toString()
            val password = binding.txtSignupPass.text.toString()
            val confirm_password = binding.txtConfirmSignupPass.text.toString()

            signup_firebase(email, password, confirm_password)
        }

        binding.tvSignin.setOnClickListener {
            val intent = Intent(this,SignInActivity::class.java)
            startActivity(intent)
        }
    }

    private fun signup_firebase(email: String, password: String, confirm_password: String){
        if(email.isNotEmpty()&& password.isNotEmpty()&& confirm_password.isNotEmpty()){
            if(password == confirm_password){
                firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
                    if(it.isSuccessful) {
                        val intent = Intent(this,SignInActivity::class.java)
                        startActivity(intent)
                    }
                    else {Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()}
                }
            }
            else{
                Toast.makeText(this,"Samakan Password dan Konfirmasi Password",Toast.LENGTH_SHORT).show()
            }
        }
        else{
            Toast.makeText(this,"Lengkapi Input",Toast.LENGTH_SHORT).show()
        }
    }
}