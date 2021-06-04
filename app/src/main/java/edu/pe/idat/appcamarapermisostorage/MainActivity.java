package edu.pe.idat.appcamarapermisostorage;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import edu.pe.idat.appcamarapermisostorage.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private static final int CAMERA_REQUEST= 1880;
    String mRutaFotoActual;
    FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        storage = FirebaseStorage.getInstance();
        binding.btntomarfoto.setOnClickListener(view ->{
            if(PermisoEscrituraAlmacenamiento()){
                intencionTomarFoto();
            }else{
                requestStoragePermission();
            }
        });
        binding.btncompartir.setOnClickListener(view ->{
            if (mRutaFotoActual != ""){
                Uri contenidoUrl = FileProvider.getUriForFile(
                        getApplicationContext(),
                        "edu.pe.idat.appcamarapermisostorage.provider",
                        new File(mRutaFotoActual)
                );
                Intent enviarIntent = new Intent();
                enviarIntent.setAction(Intent.ACTION_SEND);
                enviarIntent.putExtra(Intent.EXTRA_STREAM, contenidoUrl);
                enviarIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                enviarIntent.setType("image/jpeg");
                Intent eleccionIntent =
                        Intent.createChooser(enviarIntent, "Compartir Imagen");
                if(enviarIntent.resolveActivity(getPackageManager()) != null){
                    startActivity(eleccionIntent);
                }
            }
        });

        binding.btnstoragefirebase.setOnClickListener(view ->{
            StorageReference storageRef = storage.getReference();
            Uri file = Uri.fromFile(new File(mRutaFotoActual));
            StorageReference riversRef = storageRef.child("imagenes/"+file.getLastPathSegment());
            UploadTask uploadTask = riversRef.putFile(file);
            // Register observers to listen for when the download is done or if it fails
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Toast.makeText(MainActivity.this,
                            "Error al subir imagen",
                            Toast.LENGTH_SHORT).show();
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Toast.makeText(MainActivity.this,
                            "Imagen subida a Cloud Storage",
                            Toast.LENGTH_SHORT).show();
                }
            });

        });

    }

    private boolean PermisoEscrituraAlmacenamiento(){
        //Se obtiene si la aplicación tiene el permiso solicitado.
        int result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        boolean exito = false;
        if(result == PackageManager.PERMISSION_GRANTED){
            exito = true;
        }
        return exito;
    }

    private void requestStoragePermission(){
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission
                        .WRITE_EXTERNAL_STORAGE},
                0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0) {
            if (grantResults.length > 0
                    && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED) {
                intencionTomarFoto();
            } else {
                Toast.makeText(MainActivity.this,
                        "Permiso DENEGADO",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void intencionTomarFoto(){
        Intent takePictureIntent =
                new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //Preguntamos si existe una actividad de cámara para manejar
        //el intent.
        if(takePictureIntent.resolveActivity(getPackageManager())
                != null){
            File photoFile = null;
            try {
                photoFile = crearArchivoImagen();
            }catch (IOException ex){

            }
            //Validamos si el archivo fue creado correctamente
            if(photoFile != null){
                Uri phoURI = FileProvider.getUriForFile(
                        this,
                        "edu.pe.idat.appcamarapermisostorage.provider",
                        photoFile
                );
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT
                        , phoURI);
                startActivityForResult(takePictureIntent,
                        CAMERA_REQUEST);
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        grabarFotoGaleria();
        mostrarFoto();
    }
    private File crearArchivoImagen() throws IOException {
        String timeStamp = new SimpleDateFormat(
                "yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_"+timeStamp+"_";
        File storageDir = getExternalFilesDir(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        mRutaFotoActual = image.getAbsolutePath();
        return image;
    }
    private void grabarFotoGaleria(){
        Intent mediaScanIntent = new Intent(
                MediaStore.ACTION_IMAGE_CAPTURE);
        File nuevoArchivo = new File(mRutaFotoActual);
        Uri contentUri;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            contentUri = FileProvider.getUriForFile(
                    getApplicationContext(),
                    "edu.pe.idat.appcamarapermisostorage.provider",
                    nuevoArchivo
            );
        }else{
            contentUri = Uri.fromFile(nuevoArchivo);
        }
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void mostrarFoto() {
        ExifInterface exifInterface = null;
        try {
            exifInterface = new ExifInterface(mRutaFotoActual);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Integer orientacion = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
        );
        if(orientacion == ExifInterface.ORIENTATION_ROTATE_90){
            binding.ivfoto.setRotation(90.0F);
        }else{
            binding.ivfoto.setRotation(0.0F);
        }
        //String fecha = exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
        //Declaramos y obtenemos las dimensiones del ImageView
        int targetW = binding.ivfoto.getWidth();
        int targetH = binding.ivfoto.getHeight();

        BitmapFactory.Options bmOption = new BitmapFactory.Options();
        bmOption.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mRutaFotoActual, bmOption);
        int photoW = bmOption.outWidth;
        int photoH = bmOption.outHeight;
        //Determinar la escala de la imagen
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        // Decodificar el archivo en un mapa de bits al tamaño del
        // ImageView
        bmOption.inJustDecodeBounds = false;
        bmOption.inSampleSize = scaleFactor;
        bmOption.inPurgeable = true;
        // Asignar el archivo de la imagen en el imageview
        Bitmap bitmap = BitmapFactory.decodeFile(mRutaFotoActual,
                bmOption);
        binding.ivfoto.setImageBitmap(bitmap);
    }




}