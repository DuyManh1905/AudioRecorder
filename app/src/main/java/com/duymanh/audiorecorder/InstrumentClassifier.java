package com.duymanh.audiorecorder;

import static java.lang.Double.MAX_VALUE;

import android.content.Context;
import android.util.Log;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

public class InstrumentClassifier {

    public static final String[] labels = new String[]{"Piano","Guitar","Dan Bau","Dan tranh"};
    int [] inputShapes = new int[]{431,13};
    private OrtEnvironment ortEnvironment = OrtEnvironment.getEnvironment();
    private OrtSession ortSession;
    private Context context;

    public InstrumentClassifier(Context context) {
        OrtSession.SessionOptions sessionOption = new OrtSession.SessionOptions();
        try {
            this.context = context;
            ortSession = ortEnvironment.createSession(readModel(),sessionOption);
        } catch (OrtException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] readModel() throws IOException {
        try (InputStream inputStream = context.getAssets().open("converted_model_final.onnx");
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

            // Đọc dữ liệu vào ByteArrayOutputStream
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }

            // Trả về mảng byte đã đọc
            System.out.println("Load model thanh cong");
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            System.err.println("Error reading model: " + e.getMessage());
            throw e; // Có thể ném ngoại lệ hoặc xử lý theo cách khác
        }
    }


    public void release() {
        try {
            ortSession.close();
            ortEnvironment.close();
        } catch (OrtException e) {
            throw new RuntimeException(e);
        }
    }

    public int inference(String path){
        String outputTemp = path.substring(0, path.indexOf(".wav"))+"_temp.wav";
        FFmpegKit.execute("-i "+path+" -ar 22050 -ac 1 "+outputTemp);

        Log.i("bacnv", "inference: "+path);
        Python python = Python.getInstance();
        PyObject pyObject = python.getModule("script");
        PyObject result = pyObject.callAttr("main",outputTemp);

        List<PyObject> mfccList = result.asList();

        float [] flattenData = flattenData(mfccList);

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < flattenData.length; i++) {
            stringBuilder.append(flattenData[i]+" ");
        }
        Log.i("bacnv", stringBuilder.toString());

        try {
            OnnxTensor onnxTensor = OnnxTensor.createTensor(ortEnvironment, FloatBuffer.wrap(flattenData),new long[]{1,431,13,1});
            Map<String,OnnxTensor> inputModel = new HashMap<>();
            inputModel.put("input",onnxTensor);

            OrtSession.Result output = ortSession.run(inputModel);
            OnnxTensor outputOnnxTensor = (OnnxTensor) output.get(0);

            float[] outputArray = outputOnnxTensor.getFloatBuffer().array();
            Log.i("bacnv", "inference: "+outputArray.length);

            for(int i=0;i<outputArray.length;i++){
                System.out.println("outputArray["+i+"]: "+outputArray[i]);
            }

            int maxScoreIndex = argmax(outputArray);


            Log.i("bacnv", "Result predict - index: "+maxScoreIndex);
            Log.i("bacnv", "Result predict: "+labels[maxScoreIndex]);
            new File(outputTemp).delete();
            return maxScoreIndex;
        } catch (OrtException e) {
            throw new RuntimeException(e);
        }
    }

    private int argmax(float[] array) {
        int maxIdx = 0;
        double maxVal = -MAX_VALUE;
        for (int j = 0; j < array.length; j++) {
            if (array[j] > maxVal) {
                maxVal = array[j];
                maxIdx = j;
            }
        }
        return maxIdx;
    }

    private float[] flattenData(List<PyObject> mfccList) {
        int index = 0;
        float[] res = new float[inputShapes[0]*inputShapes[1]];
        for (int i = 0; i < inputShapes[0] ; i++) {
            List<PyObject> mfccListSub = mfccList.get(i).asList();
            for (int j = 0; j < inputShapes[1]; j++) {
                res[index] = mfccListSub.get(j).toFloat();
                index += 1;
            }
        }
        return res;
    }
}