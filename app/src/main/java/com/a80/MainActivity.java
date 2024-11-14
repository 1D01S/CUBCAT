package com.a80;

import android.content.Intent;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_MODEL_REQUEST = 1;
    private GLSurfaceView glSurfaceView;
    private CubeRenderer cubeRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        glSurfaceView = findViewById(R.id.gl_surface_view);
        glSurfaceView.setEGLContextClientVersion(2);
        cubeRenderer = new CubeRenderer(new TouchHandler(this));
        glSurfaceView.setRenderer(cubeRenderer);

        glSurfaceView.setOnTouchListener((v, event) -> {
            cubeRenderer.getTouchHandler().onTouchEvent(event);
            return true;
        });

        Button loadModelButton = findViewById(R.id.load_model_button);
        loadModelButton.setOnClickListener(v -> openFileChooser());

        Button unloadModelButton = findViewById(R.id.unload_model_button);
        unloadModelButton.setOnClickListener(v -> cubeRenderer.unloadModels()); // Разгрузка всех моделей
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/octet-stream");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select .obj file"), PICK_MODEL_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_MODEL_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                loadModelFromUri(uri);
            }
        }
    }

    private void loadModelFromUri(Uri uri) {
        try {
            InputStream objInputStream = getContentResolver().openInputStream(uri);
            if (objInputStream != null) {
                glSurfaceView.queueEvent(() -> {
                    cubeRenderer.loadModel(this, objInputStream);
                    try {
                        objInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        glSurfaceView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        glSurfaceView.onResume();
    }
}
