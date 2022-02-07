package com.dicoding.myreactiveform

import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dicoding.myreactiveform.databinding.ActivityMainBinding
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val emailStream = RxTextView.textChanges(binding.edEmail)
            .skipInitialValue()
            .map { email ->
                !Patterns.EMAIL_ADDRESS.matcher(email).matches()
            }

        val emailDispose = emailStream.subscribe {
            showEmailExistAlert(it)
        }

        val passwordStream = RxTextView.textChanges(binding.edPassword)
            .skipInitialValue()
            .map { password ->
                password.length < 6
            }

        val passwordDispose = passwordStream.subscribe {
            showPasswordMinimalAlert(it)
        }

        val passwordConfirmationStream = Observable.merge(
            RxTextView.textChanges(binding.edPassword)
                .map { password ->
                    password.toString() != binding.edConfirmPassword.text.toString()
                },
            RxTextView.textChanges(binding.edConfirmPassword)
                .map { confirmationPassword ->
                    confirmationPassword.toString() != binding.edPassword.text.toString()
                }
        )

        val passwordConfirmationDispose = passwordConfirmationStream.subscribe {
            showPasswordConfirmationAlert(it)
        }

        val invalidFieldsStream = Observable.combineLatest(
            emailStream,
            passwordStream,
            passwordConfirmationStream,
            { emailValid, passwordValid, passwordConfirmationValid ->
                !emailValid && !passwordValid && !passwordConfirmationValid
            }
        )

        val invalidFieldsDispose = invalidFieldsStream.subscribe { isValid ->
            if(isValid){
                binding.btnRegister.isEnabled = true
                binding.btnRegister.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_500))
            } else {
                binding.btnRegister.isEnabled = false
                binding.btnRegister.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))
            }
        }

        compositeDisposable.addAll(emailDispose, passwordDispose, passwordConfirmationDispose, invalidFieldsDispose)
    }

    private fun showEmailExistAlert(isNotValid: Boolean) {
        binding.edEmail.error = if (isNotValid) getString(R.string.email_not_valid) else null
    }

    private fun showPasswordMinimalAlert(isNotValid: Boolean) {
        binding.edPassword.error = if (isNotValid) getString(R.string.password_not_valid) else null
    }

    private fun showPasswordConfirmationAlert(isNotValid: Boolean) {
        binding.edConfirmPassword.error =
            if (isNotValid) getString(R.string.password_not_same) else null
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
    }
}
