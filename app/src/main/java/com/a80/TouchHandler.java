package com.a80;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.NonNull;

// Класс для обработки касаний и жестов
public class TouchHandler {
    private final ScaleGestureDetector scaleGestureDetector; // Обработчик жестов масштабирования
    private final GestureDetector gestureDetector; // Обработчик жестов
    private float rotationX = 0, rotationY = 0, scale = 1.0f; // Угол вращения и масштаб

    // Конструктор класса обработчика касаний
    public TouchHandler(Context context) {
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public boolean onScale(@NonNull ScaleGestureDetector detector) {
                scale *= detector.getScaleFactor(); // Изменяем масштаб
                scale = Math.max(0.1f, Math.min(scale, 80.0f)); // Ограничиваем масштаб
                return true;
            }

            @Override
            public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
                return true; // Начало масштабирования
            }

            @Override
            public void onScaleEnd(@NonNull ScaleGestureDetector detector) {} // Конец масштабирования
        });

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
                rotationX -= distanceY; // Инвертируем направление вращения по X
                rotationY += distanceX; // Оставляем направление вращения по Y
                return true;
            }
        });
    }

    // Метод для обработки событий касания
    public void onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event); // Обрабатываем масштабирование
        gestureDetector.onTouchEvent(event); // Обрабатываем другие жесты
    }

    // Получаем угол вращения по оси X
    public float getRotationX() {
        return rotationX;
    }

    // Получаем угол вращения по оси Y
    public float getRotationY() {
        return rotationY;
    }

    // Получаем масштаб
    public float getScale() {
        return scale;
    }
}