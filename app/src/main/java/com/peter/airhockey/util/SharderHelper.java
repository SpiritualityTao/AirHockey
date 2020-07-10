package com.peter.airhockey.util;

import android.util.Log;

import static android.opengl.GLES20.*;


public class SharderHelper {

    private static final String TAG = "SharderHelper";

    public static int compileVertexSharder(String shaderCode) {
        return compileSharder(GL_VERTEX_SHADER, shaderCode);
    }

    public static int compileFragmentSharder(String shaderCode) {
        return compileSharder(GL_FRAGMENT_SHADER, shaderCode);
    }

    public static int compileSharder(int type, String shaderCode){
        //1.创建一个新的着色器对象
        final int shaderObjectId = glCreateShader(type);

        if(shaderObjectId == 0) {
            if(LoggerConfig.ON) {
                Log.w(TAG, "Couldn't create new sharder ");
            }
            return 0;
        }

        //2.上传和编译着色器源代码
        glShaderSource(shaderObjectId, shaderCode);
        glCompileShader(shaderObjectId);

        //3.去除编译状态
        final int[] compileStatus = new int[1];
        glGetShaderiv(shaderObjectId, GL_COMPILE_STATUS, compileStatus, 0);

        //4.去除着色器信息日志
        if(LoggerConfig.ON) {
            Log.v(TAG, "Results compiling source :\n" + shaderCode + "\n" + glGetShaderInfoLog(shaderObjectId));
        }

        //5.验证着色器编译状态并且返回着色器对象ID
        if(compileStatus[0] == 0) {
            // if it failed,delete shader object
            glDeleteShader(shaderObjectId);
            if(LoggerConfig.ON) {
                Log.w(TAG, "Compilation of sharder failed.");
            }
            return 0;
        }

        return shaderObjectId;
    }

    public static int linkProgram(int vertexshaderId, int fragmentShaderId) {

        final int programObjectId = glCreateProgram();

        if(programObjectId == 0) {
            if(LoggerConfig.ON) {
                Log.w(TAG, "Couldn't create new program ");
            }
            return 0;
        }

        glAttachShader(programObjectId, vertexshaderId);
        glAttachShader(programObjectId, fragmentShaderId);

        glLinkProgram(programObjectId);

        final int[] linkStatus = new int[1];
        glGetProgramiv(programObjectId, GL_LINK_STATUS, linkStatus, 0);

        if(linkStatus[0] == 0) {
            // if it failed,delete shader object
            glDeleteProgram(programObjectId);
            if(LoggerConfig.ON) {
                Log.w(TAG, "Linking of program failed.");
            }
            return 0;
        }

        return programObjectId;

    }

    public static boolean validateProgram(int programObjectId) {

        glValidateProgram(programObjectId);

        final int[] validateStatus = new int[1];

        glGetProgramiv(programObjectId, GL_VALIDATE_STATUS, validateStatus, 0);

        Log.v(TAG, "Results of validating program: " + validateStatus[0]
                + "\nLog" + glGetProgramInfoLog(programObjectId));

        return validateStatus[0] != 0;
    }
}
