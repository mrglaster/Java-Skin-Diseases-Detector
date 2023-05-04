package com.example.skin_diseases_detection;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    public static final String API_KEY = "eda14dc107msh246b8d7e64865dep1aa8d6jsnf3b6f12d24c1";
    public static final String API_HOST = "detect-skin-disease.p.rapidapi.com";
    public static final String BASE_URL = "https://detect-skin-disease.p.rapidapi.com/";

    public static byte[] diseaseImage = null;

    public static ImageView uploadedDemo = null;

    @SuppressLint("QueryPermissionsNeeded")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button camButton = (Button) findViewById(R.id.button_camera);
        Button galleryButton = (Button) findViewById(R.id.button_galery);
        Button buttonSubmit = (Button) findViewById(R.id.buttonSubmit);
        uploadedDemo = findViewById(R.id.imageView);


        /*Gallery opening intent*/
        galleryButton.setOnClickListener(v -> {
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, 1);
        });

        /*Camera access intent*/
        camButton.setOnClickListener(v -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, 2);
            }
        });


        /*Send request to the server*/
        buttonSubmit.setOnClickListener(v -> {
            if (diseaseImage != null){
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
                SkinDiseaseAPI api = retrofit.create(SkinDiseaseAPI.class);
                RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), diseaseImage);

                Call<ResponseBody> call = api.detectSkinDisease(
                        requestFile,
                        API_KEY,
                        API_HOST
                );
                generateToast("Sending request...");
                call.enqueue(new Callback<ResponseBody>() {

                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {

                        if (response.code() == 429){
                            generateToast("Your API KEY is expired or you exceeded the requests limit!");
                        }
                        else if (response.code() == 403){
                            generateToast("Forbidden! Wrong API key!");
                        }
                        else if (response.code() == 400){
                            generateToast("Bad Request!");

                        }
                        else if (response.isSuccessful()) {
                            try {
                                // JSON parsing & results demonstration
                                String responseString = Objects.requireNonNull(response.body()).string();
                                Log.d("API Response", "Response Body: " + responseString);
                                JsonObject responseJson = new Gson().fromJson(responseString, JsonObject.class);
                                JsonObject globalData = responseJson.get("data").getAsJsonObject();
                                JsonObject englishResults = globalData.get("results_english").getAsJsonObject();
                                StringBuilder resultString = new StringBuilder("\nPossible diseases: \n");
                                for (String key : englishResults.keySet()){
                                    String processedKey = key.replace("_", " ");
                                    double value = englishResults.get(key).getAsDouble();
                                    int percentage = (int)(value*100);
                                    resultString.append(processedKey).append(" (").append(percentage).append("%)").append("\n");
                                }
                                Log.d("Formatted response", resultString.toString());
                                TextView resultView = findViewById(R.id.diagnosisText);
                                resultView.setText(resultString.toString());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Log.e("API Response", "Response Code: " + response.code());
                            generateToast("Some unexpected error happened during the connection...");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                        generateToast("Something went wrong during the request! Check the internet connection or other stuff!");
                    }
                });
            } else {
                generateToast("You should make the image via camera or select one from the gallery!");
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {

            switch (requestCode){

                /*Processing gallery loading*/
                case  1:
                    processGalleryStage(data);
                    break;

                /*Processing the image from the camera*/
                case 2:
                    processCameraStage(data);
                    break;

            }
        }
    }

    /**Generates Toasts with short show-time-duration*/
    public void generateToast(String message){
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, message, duration);
        toast.show();
    }

    /**Processing the data we got from the camera*/
    public static void processCameraStage(Intent data){
        Bitmap bitmap = (Bitmap) data.getExtras().get("data");
        uploadedDemo.setImageBitmap(bitmap);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        diseaseImage = byteArrayOutputStream.toByteArray();
    }

    /**Processing the data we got from the gallery*/
    public void processGalleryStage(Intent data){
        Uri selectedImage = data.getData();
        try {
            InputStream inputStream = getContentResolver().openInputStream(selectedImage);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            uploadedDemo.setImageBitmap(bitmap);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            diseaseImage = byteArrayOutputStream.toByteArray();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


}
