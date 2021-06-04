package edu.pe.idat.appcamarapermisostorage;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import edu.pe.idat.appcamarapermisostorage.databinding.ActivityLoginBinding;
import edu.pe.idat.appcamarapermisostorage.databinding.ActivityMainBinding;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mAuth = FirebaseAuth.getInstance();
        binding.btnloginfirebase.setOnClickListener(view ->{
            if(binding.etemail.getText().toString().equals("") &&
            binding.etpassword.getText().toString().equals("")){
                Toast.makeText(getApplicationContext(),
                        "Ingrese su email y password.",
                        Toast.LENGTH_SHORT).show();
            }else {
                mAuth.signInWithEmailAndPassword(binding.etemail.getText().toString(),
                        binding.etpassword.getText().toString())
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    startActivity(new Intent(getApplicationContext(),
                                            MainActivity.class));
                                } else {
                                    Toast.makeText(getApplicationContext(),
                                            "Email o password incorrecto.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });


            }
        });
    }
}