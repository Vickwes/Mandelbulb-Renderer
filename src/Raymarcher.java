/*
 * JOCL - Java bindings for OpenCL
 *
 * Copyright 2009-2019 Marco Hutter - http://www.jocl.org/
 */

import static org.jocl.CL.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.Buffer;
import java.util.Arrays;

import org.jocl.*;

import javax.swing.*;

/**
 * A small JOCL sample.
 */
public class Raymarcher extends JComponent
{
    /**
     * The source code of the OpenCL program to execute
     */
    final static int SIZE = 700; //this has to align with the fragment
    final static int n = SIZE * SIZE;
    private String programSource;
    private short rArray[];
    private short gArray[];
    private short bArray[];
    private cl_mem rMem;
    private cl_mem gMem;
    private cl_mem bMem;
    private cl_program program;
    private cl_kernel kernel;
    private cl_command_queue commandQueue;
    private Pointer r;
    private Pointer g;
    private Pointer b;
    private cl_context context;




    @Override
    public void paint(Graphics g){
        g.drawImage(raymarch(), 0, 0, null);
    }
    public Raymarcher(){
        // Create input- and output data

        rArray = new short[n];
        gArray = new short[n];
        bArray = new short[n];
        r = Pointer.to(rArray);
        g = Pointer.to(gArray);
        b = Pointer.to(bArray);
        setPreferredSize(new Dimension(SIZE, SIZE));
        programSource = readFile("src/kernel.cl");
        // The platform, device type and device number
        // that will be used
        final int platformIndex = 0;
        final long deviceType = CL_DEVICE_TYPE_ALL;
        final int deviceIndex = 0;

        // Enable exceptions and subsequently omit error checks in this sample
        CL.setExceptionsEnabled(true);

        // Obtain the number of platforms
        int numPlatformsArray[] = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];


        // Obtain a platform ID
        cl_platform_id platforms[] = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);
        cl_platform_id platform = platforms[platformIndex];

        // Initialize the context properties
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);

        // Obtain the number of devices for the platform
        int numDevicesArray[] = new int[1];
        clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];

        // Obtain a device ID
        cl_device_id devices[] = new cl_device_id[numDevices];
        clGetDeviceIDs(platform, deviceType, numDevices, devices, null);
        cl_device_id device = devices[deviceIndex];

        // Create a context for the selected device
        context = clCreateContext(
                contextProperties, 1, new cl_device_id[]{device},
                null, null, null);

        // Create a command-queue for the selected device
        cl_queue_properties properties = new cl_queue_properties();
        commandQueue = clCreateCommandQueueWithProperties(
                context, device, properties, null);

        // Allocate the memory objects for the input- and output data
        rMem = clCreateBuffer(context,
                CL_MEM_READ_WRITE,
                Sizeof.cl_short * n, null, null);
        gMem = clCreateBuffer(context,
                CL_MEM_READ_WRITE,
                Sizeof.cl_short * n, null, null);
        bMem = clCreateBuffer(context,
                CL_MEM_READ_WRITE,
                Sizeof.cl_short * n, null, null);

        // Create the program from the source code
        program = clCreateProgramWithSource(context,
                1, new String[]{ programSource }, null, null);

        // Build the program
        clBuildProgram(program, 0, null, null, null, null);

        // Create the kernel
        kernel = clCreateKernel(program, "sampleKernel", null);

    }

    public BufferedImage raymarch()
    {



        // Set the arguments for the kernel
        float angle = (float)Math.toDegrees(System.nanoTime()/100000000000d);
        // Rotates the camera over time
        float[] ro = new float[]{(float)(-2*Math.sin(angle)),0,(float)(-2*Math.cos(angle))};
        float x = -1;
        float z = 0;
        float[] inputAngle = new float[]{(float)(x * Math.cos(angle) + z * Math.sin(angle)),(float)(-(x)*Math.sin(angle) + z * Math.cos(angle))};
        float[] power = new float[]{(float)Math.sin(System.nanoTime()/5000000000d) *2f+ 7f};
        int a = 0;
        clSetKernelArg(kernel, a++, Sizeof.cl_mem, Pointer.to(rMem));
        clSetKernelArg(kernel, a++, Sizeof.cl_mem, Pointer.to(gMem));
        clSetKernelArg(kernel, a++, Sizeof.cl_mem, Pointer.to(bMem));
        clSetKernelArg(kernel, a++, Sizeof.cl_float3, Pointer.to(ro));
        clSetKernelArg(kernel, a++, Sizeof.cl_float2, Pointer.to(inputAngle));
        clSetKernelArg(kernel, a++, Sizeof.cl_float2, Pointer.to(power));


        // Set the work-item dimensions
        long global_work_size[] = new long[]{n};

        // Execute the kernel
        clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
                global_work_size, null, 0, null, null);

        // Read the output data
        clEnqueueReadBuffer(commandQueue, rMem, CL_TRUE, 0,
                n * Sizeof.cl_short, r, 0, null, null);
        clEnqueueReadBuffer(commandQueue, gMem, CL_TRUE, 0,
                n * Sizeof.cl_short, g, 0, null, null);
        clEnqueueReadBuffer(commandQueue, bMem, CL_TRUE, 0,
                n * Sizeof.cl_short, b, 0, null, null);



        // Process the result
        BufferedImage bi = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
        for(int i = 0; i < SIZE; i++){
            for(int j = 0; j < SIZE; j++){
                int index = i * SIZE + j;
                bi.setRGB(j,i, new Color(rArray[index],gArray[index],bArray[index]).getRGB());
            }
        }
        return bi;
    }
    //Helper method to read from kernel.cl
    private static String readFile(String fileName)
    {
        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader(fileName));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while (true)
            {
                line = br.readLine();
                if (line == null)
                {
                    break;
                }
                sb.append(line+"\n");
            }
            return sb.toString();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.exit(1);
            return "";
        }
        finally
        {
            if (br != null)
            {
                try
                {
                    br.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
}