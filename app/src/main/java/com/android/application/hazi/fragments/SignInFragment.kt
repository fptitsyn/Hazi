package com.android.application.hazi.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.android.application.hazi.R
import com.android.application.hazi.databinding.FragmentSignInBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase

class SignInFragment : Fragment() {

    private lateinit var binding: FragmentSignInBinding

    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sign_in, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentSignInBinding.bind(view)

        binding.signInButton.setOnClickListener { signIn() }
        binding.createAccountButton.setOnClickListener { startSignUpFragment() }

        database = FirebaseDatabase.getInstance("https://hazi-8190a-default-rtdb.europe-west1.firebasedatabase.app/")

        auth = Firebase.auth
    }

    private fun signIn() {
        if (binding.emailEditText.text.trim().isBlank()) {
            Toast.makeText(context, "Please enter your email", Toast.LENGTH_SHORT).show()
        } else if (binding.passwordEditText.text.trim().isBlank()) {
            Toast.makeText(context, "Please enter a password", Toast.LENGTH_SHORT).show()
        } else {
            val email = binding.emailEditText.text.trim().toString()
            val password = binding.passwordEditText.text.trim().toString()

            Log.d("authStatus", "authUser")
                activity?.let {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(it) { task ->
                            if (task.isSuccessful) {
                                // Sign in success, update UI with the signed-in user's information
                                Log.d("signInUserWithEmail", "signInWithEmail:success")
                                startMainActivity()
                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w("signInUserWithEmail", "signInWithEmail:failure", task.exception)
                                Toast.makeText(context, "Invalid email or password",
                                    Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }
            Log.d("authStatus", "authSignIn")
    }

    private fun startSignUpFragment() {
        val email = binding.emailEditText.text.toString()
        val emailArg = email.ifBlank { null }

        val direction = SignInFragmentDirections.actionAuthFragmentToSignUpFragment(emailArg)
        findNavController().navigate(direction)
    }

    private fun startMainActivity() {
        Log.d("authStatus", "startMainActivity")
        findNavController().navigate(R.id.action_signInFragment_to_tabsFragment)
    }
}