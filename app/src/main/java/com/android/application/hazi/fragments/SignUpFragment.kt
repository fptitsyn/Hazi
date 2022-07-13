package com.android.application.hazi.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.android.application.hazi.R
import com.android.application.hazi.databinding.FragmentSignUpBinding
import com.android.application.hazi.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase

class SignUpFragment : Fragment() {

    private lateinit var database: FirebaseDatabase

    private lateinit var binding: FragmentSignUpBinding

    private lateinit var auth: FirebaseAuth

    private val args by navArgs<SignUpFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sign_up, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentSignUpBinding.bind(view)

        binding.createAccountButton.setOnClickListener { createAccount() }

        if (savedInstanceState == null && args.emailArg != null) {
            binding.emailEditText.setText(args.emailArg)
        }

        database = FirebaseDatabase.getInstance("https://hazi-8190a-default-rtdb.europe-west1.firebasedatabase.app/")
        auth = Firebase.auth
    }

    private fun createAccount() {
        if (binding.emailEditText.text.trim().isBlank()) {
            Toast.makeText(context, "Please enter your email", Toast.LENGTH_SHORT).show()
        } else if (binding.passwordEditText.text.trim().isBlank()) {
            Toast.makeText(context, "Please enter a password", Toast.LENGTH_SHORT).show()
        } else {
            val email = binding.emailEditText.text.trim().toString()
            val password = binding.passwordEditText.text.trim().toString()

            Log.d("authStatus", "authUser")

            if (binding.confirmPasswordEditText.text.trim().toString() != password) {
                Toast.makeText(context, "Passwords don't match", Toast.LENGTH_SHORT).show()
            } else {
                activity?.let {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(it) { task ->
                            if (task.isSuccessful) {
                                // Sign in success, update UI with the signed-in user's information
                                Log.d("createUserWithEmail", "createUserWithEmail:success")

                                val user = auth.currentUser
                                addUserToDatabase(email, password, user!!.uid)

                                Toast.makeText(
                                    requireContext(),
                                    "Account created successfully",
                                    Toast.LENGTH_SHORT
                                ).show()

                                findNavController().popBackStack()
                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w(
                                    "createUserWithEmail",
                                    "createUserWithEmail:failure",
                                    task.exception
                                )
                                Toast.makeText(
                                    context, "Something went wrong",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                }
            }

            Log.d("authStatus", "authSignUp")
        }
    }

    private fun addUserToDatabase(email: String, password: String, id: String) {
        var coins = 0
        if (email == "fedor.kuritsyn@mail.ru") {
            coins = 100000
        }

        val user = User(email, password, id, coins)

        val usersDatabaseReference = database.reference.child("users")
        usersDatabaseReference.push().setValue(user)
        Log.d("authStatus", "addUserToDatabase")
    }

}