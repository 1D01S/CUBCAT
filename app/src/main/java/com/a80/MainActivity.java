package com.a80;

import android.content.Intent;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;

// Главный класс активности приложения
public class MainActivity extends AppCompatActivity {
    private static final int PICK_MODEL_REQUEST = 1; // Константа для запроса выбора модели
    private GLSurfaceView glSurfaceView; // Поле для отображения 3D-графики
    private CubeRenderer cubeRenderer; // Рендерер для отрисовки куба
    private boolean isModelLoaded = false; // Флаг, указывающий, загружена ли модель

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Устанавливаем интерфейс

        // Находим представление для OpenGL
        glSurfaceView = findViewById(R.id.gl_surface_view);
        glSurfaceView.setEGLContextClientVersion(2); // Устанавливаем версию OpenGL
        cubeRenderer = new CubeRenderer(new TouchHandler(this)); // Инициализируем рендерер
        glSurfaceView.setRenderer(cubeRenderer); // Устанавливаем рендерер для поверхности

        // Устанавливаем обработчик жестов для поверхности
        glSurfaceView.setOnTouchListener((v, event) -> {
            cubeRenderer.getTouchHandler().onTouchEvent(event); // Обработка касания
            return true;
        });

        // Кнопка для загрузки 3D-модели
        Button loadModelButton = findViewById(R.id.load_model_button);
        loadModelButton.setOnClickListener(v -> openFileChooser()); // Открыть файл выбора

        // Кнопка для выгрузки 3D-модели
        Button unloadModelButton = findViewById(R.id.unload_model_button);
        unloadModelButton.setEnabled(false); // Сначала отключаем кнопку
        unloadModelButton.setOnClickListener(v -> {
            glSurfaceView.queueEvent(cubeRenderer::unloadModels); // Запрос на выгрузку модели
            isModelLoaded = false; // Устанавливаем состояние при выгрузке
            unloadModelButton.setEnabled(false); // Деактивируем кнопку
        });

        // Переключатель для режима отображения в Wireframe
        Switch wireframeSwitch = findViewById(R.id.wireframe_switch);
        wireframeSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                glSurfaceView.queueEvent(() -> cubeRenderer.setWireframeMode(isChecked)) // Устанавливаем режим
        );
    }

    // Метод для открытия выборщика файлов
    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/octet-stream"); // Задаем тип файла
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select .obj file"), PICK_MODEL_REQUEST); // Запускаем выбор файла
    }

    // Обработка результата выбора файла
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_MODEL_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData(); // Получаем URI выбранного файла
            if (uri != null) {
                loadModelFromUri(uri); // Загружаем модель из URI
            }
        }
    }

    // Метод для загрузки 3D-модели из URI
    private void loadModelFromUri(Uri uri) {
        try {
            InputStream objInputStream = getContentResolver().openInputStream(uri); // Открываем поток
            if (objInputStream != null) {
                glSurfaceView.queueEvent(() -> {
                    cubeRenderer.loadModel(objInputStream); // Загружаем модель в рендерер
                    try {
                        objInputStream.close(); // Закрываем поток
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // Возвращаемся к основному потоку для обновления интерфейса
                    runOnUiThread(() -> {
                        isModelLoaded = true; // Устанавливаем флаг загрузки
                        findViewById(R.id.unload_model_button).setEnabled(true); // Активируем кнопку выгрузки
                    });
                });
            }
        } catch (IOException e) {
            e.printStackTrace(); // Обработка ошибок ввода-вывода
        }
    }

    // Пауза активности
    @Override
    protected void onPause() {
        super.onPause();
        glSurfaceView.onPause(); // Пауза в рендерере
    }

    // Возобновление активности
    @Override
    protected void onResume() {
        super.onResume();
        glSurfaceView.onResume(); // Возобновление рендерера
    }
}