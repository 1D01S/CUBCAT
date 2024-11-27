package com.a80;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

// Класс рендерера, отвечающий за рисование куба и загрузку моделей
public class CubeRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "CubeRenderer"; // Метка для логирования
    private final List<Model> models = new ArrayList<>(); // Список моделей для отрисовки
    private final TouchHandler touchHandler; // Обработчик касаний

    private final float[] projectionMatrix = new float[16]; // Матрица проекции
    private final float[] viewMatrix = new float[16]; // Матрица вида
    private final float[] modelMatrix = new float[16]; // Матрица модели
    private boolean wireframeMode = false; // Режим отображения в Wireframe

    // Конструктор класса рендерера
    public CubeRenderer(TouchHandler touchHandler) {
        this.touchHandler = touchHandler; // Инициализируем обработчик касаний
        models.add(createCube()); // Создаем куб и добавляем в список моделей
    }

    // Получаем обработчик касаний
    public TouchHandler getTouchHandler() {
        return touchHandler;
    }

    // Устанавливаем режим Wireframe
    public void setWireframeMode(boolean wireframeMode) {
        this.wireframeMode = wireframeMode;
    }

    // Загружаем модель из входного потока
    public void loadModel(InputStream objInputStream) {
        try {
            Model model = parseObj(objInputStream); // Парсим OBJ-файл
            models.clear(); // Очищаем список моделей
            models.add(model); // Добавляем загруженную модель
            Log.d(TAG, "Model loaded successfully."); // Логируем успех
        } catch (Exception e) {
            Log.e(TAG, "Error loading model: " + e.getMessage(), e); // Логируем ошибку загрузки
        }
    }

    // Выгружаем все модели
    public void unloadModels() {
        models.clear(); // Очищаем список моделей
        models.add(createCube()); // Добавляем куб обратно
        Log.d(TAG, "All models unloaded."); // Логируем выгрузку
    }

    // Метод, вызываемый при создании поверхности для рендеринга
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0, 0, 0, 1); // Устанавливаем цвет фона
        GLES20.glEnable(GLES20.GL_DEPTH_TEST); // Включаем тест глубины
        Matrix.setLookAtM(viewMatrix, 0, 0, 0, 20, 0, 0, 0, 0, 1, 0); // Устанавливаем матрицу вида
    }

    // Метод, вызываемый для отрисовки каждого кадра
    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT); // Очищаем буферы

        Matrix.setIdentityM(modelMatrix, 0); // Обнуляем матрицу модели
        Matrix.translateM(modelMatrix, 0, 0, 0, -10); // Перемещаем модель
        Matrix.rotateM(modelMatrix, 0, touchHandler.getRotationX(), 1, 0, 0); // Вращаем по оси X
        Matrix.rotateM(modelMatrix, 0, touchHandler.getRotationY(), 0, 1, 0); // Вращаем по оси Y
        Matrix.scaleM(modelMatrix, 0, touchHandler.getScale(), touchHandler.getScale(), touchHandler.getScale()); // Масштабируем модель

        float[] mvpMatrix = new float[16]; // Матрица MVP для отрисовки
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0); // Умножаем проекцию на вид
        Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, modelMatrix, 0); // Умножаем на матрицу модели

        for (Model model : models) {
            model.draw(mvpMatrix, wireframeMode); // Отрисовываем каждую модель
        }
    }

    // Метод, вызываемый при изменении размеров поверхности
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height); // Устанавливаем размер области рендеринга
        float aspectRatio = (float) width / height; // Вычисляем соотношение сторон
        Matrix.frustumM(projectionMatrix, 0, -aspectRatio, aspectRatio, -1, 1, 2, 100); // Устанавливаем матрицу проекции
    }

    // Метод для создания куба
    private Model createCube() {
        float[] vertices = {
                -1, -1, 1,  1, -1, 1,  1, 1, 1,  -1, 1, 1,
                -1, -1, -1, -1, 1, -1, 1, 1, -1,  1, -1, -1,
                -1, -1, -1, -1, -1, 1, -1, 1, 1, -1, 1, -1,
                1, -1, -1, 1, -1, 1, 1, 1, 1,  1, 1, -1,
                -1, 1, -1, -1, 1, 1, 1, 1, 1,  1, 1, -1,
                -1, -1, -1, 1, -1, -1, 1, -1, 1, -1, -1, 1
        };

        short[] indices = {
                0, 1, 2, 2, 3, 0, 4, 5, 6, 6, 7, 4,
                8, 9, 10, 10, 11, 8, 12, 13, 14, 14, 15, 12,
                16, 17, 18, 18, 19, 16, 20, 21, 22, 22, 23, 20
        };

        return new Model(vertices, indices); // Возвращаем новый объект модели куба
    }

    // Метод для парсинга OBJ-файла
    private Model parseObj(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream)); // Читаем содержимое
        List<Float> vertices = new ArrayList<>(); // Список вершин
        List<Short> indices = new ArrayList<>(); // Список индексов

        String line;
        while ((line = reader.readLine()) != null) { // Читаем файл построчно
            String[] tokens = line.split("\\s+"); // Разбиваем строку на токены
            if (tokens[0].equals("v")) { // Если токен - вершина
                vertices.add(Float.parseFloat(tokens[1])); // Добавляем координаты вершины
                vertices.add(Float.parseFloat(tokens[2]));
                vertices.add(Float.parseFloat(tokens[3]));
            } else if (tokens[0].equals("f")) { // Если токен - грань
                for (int i = 1; i <= 3; i++) { // Добавляем индексы вершин
                    String[] parts = tokens[i].split("/");
                    indices.add((short) (Integer.parseInt(parts[0]) - 1)); // Индексы начинаются с 1
                }
            }
        }

        return new Model(toFloatArray(vertices), toShortArray(indices)); // Возвращаем модель
    }

    // Преобразование списка вершин в массив
    private float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) array[i] = list.get(i); // Копируем значения
        return array; // Возвращаем массив
    }

    // Преобразование списка индексов в массив
    private short[] toShortArray(List<Short> list) {
        short[] array = new short[list.size()];
        for (int i = 0; i < list.size(); i++) array[i] = list.get(i); // Копируем значения
        return array; // Возвращаем массив
    }
}