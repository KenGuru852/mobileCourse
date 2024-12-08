package com.example.curs_work;

import androidx.appcompat.app.AppCompatActivity;

import android.opengl.GLSurfaceView;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        GLSurfaceView mGLSurfaceView = new GLSurfaceView(this);

        mGLSurfaceView.setEGLContextClientVersion(2);//устанавливаем версию OPENGL2

        MyRenderer renderer = new MyRenderer(this);//создаем свой рендерер
        mGLSurfaceView.setRenderer(renderer);

        setContentView(mGLSurfaceView);
    }
}