package com.example.curs_work;

import android.opengl.GLES20;

class ShadersWork
{
    private int mProgram;

    private final String mVertexS;
    private final String mFragmentS;

    ShadersWork(String vID, String fID)
    {
        this.mVertexS = vID;
        this.mFragmentS = fID;

        if (createProgram() != 1)
        {
            throw new RuntimeException("Error at creating shaders");
        }
    }

    private int createProgram()//загрузка шейдеров
    {
        int mVertexShader = loadShader(GLES20.GL_VERTEX_SHADER, mVertexS);
        if (mVertexShader == 0)
        {
            return 0;
        }

        int mPixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, mFragmentS);
        if (mPixelShader == 0)
        {
            return 0;
        }

        /*активация шейдеров*/
        mProgram = GLES20.glCreateProgram();
        if (mProgram != 0)
        {
            GLES20.glAttachShader(mProgram, mVertexShader);
            GLES20.glAttachShader(mProgram, mPixelShader);
            GLES20.glLinkProgram(mProgram);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(mProgram, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE)
            {
                GLES20.glDeleteProgram(mProgram);
                mProgram = 0;
                return 0;
            }
        }
        else
        {
            return -1;
        }

        return 1;
    }

    private int loadShader(int shaderType, String source)//загрузка шейдеров
    {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0)
        {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0)
            {
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    int getProgram()
    {
        return mProgram;
    }
}
