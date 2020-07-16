package com.peter.airhockey.util;

import android.util.Log;

import static android.opengl.GLES20.*;


public class SharderHelper {

    private static final String TAG = "SharderHelper";

    /**
     * 编译顶点着色器
     * @param shaderCode 着色器代码段
     * @return
     */
    public static int compileVertexSharder(String shaderCode) {
        return compileSharder(GL_VERTEX_SHADER, shaderCode);
    }

    /**
     * 编译片段着色器
     * @param shaderCode 着色器代码段
     * @return
     */
    public static int compileFragmentSharder(String shaderCode) {
        return compileSharder(GL_FRAGMENT_SHADER, shaderCode);
    }

    /**
     * 编译着色器
     * @param type GL_VERTEX_SHADER or GL_FRAGMENT_SHADER
     * @param shaderCode
     * @return
     */
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

        //3.取出编译状态
        final int[] compileStatus = new int[1];
        glGetShaderiv(shaderObjectId, GL_COMPILE_STATUS, compileStatus, 0);

        //4.取出着色器信息日志
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

    /**
     * 把着色器一起链接到OpenGL程序
     * @param vertexshaderId
     * @param fragmentShaderId
     * @return
     */
    public static int linkProgram(int vertexshaderId, int fragmentShaderId) {
        //1. 新建程序对象
        final int programObjectId = glCreateProgram();

        if(programObjectId == 0) {
            if(LoggerConfig.ON) {
                Log.w(TAG, "Couldn't create new program ");
            }
            return 0;
        }
        //2. 程序对象附上着色器
        glAttachShader(programObjectId, vertexshaderId);
        glAttachShader(programObjectId, fragmentShaderId);
        //3. 把着色器联合起来
        glLinkProgram(programObjectId);

        final int[] linkStatus = new int[1];
        //4. 取出链接状态
        glGetProgramiv(programObjectId, GL_LINK_STATUS, linkStatus, 0);
        //5. 验证链接状态并且返回着色器对象ID
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

    /**
     * 验证程序
     * @param programObjectId
     * @return
     */
    public static boolean validateProgram(int programObjectId) {

        glValidateProgram(programObjectId);

        final int[] validateStatus = new int[1];

        glGetProgramiv(programObjectId, GL_VALIDATE_STATUS, validateStatus, 0);

        Log.v(TAG, "Results of validating program: " + validateStatus[0]
                + "\nLog" + glGetProgramInfoLog(programObjectId));

        return validateStatus[0] != 0;
    }
}
