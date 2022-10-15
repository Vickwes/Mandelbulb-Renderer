inline float2 mapWorld(float3 p, float power){
    float3 z = p;
    float dr = 1;
    float r;
    int iterations = 16;
    for(int i = 0; i < 15; i++){
        iterations = i;
        r = length(z);
        if(r>2)
            break;
        float theta = acos(z.z/r) * power;
        float phi = atan2(z.y, z.x) * power;
        float zr = pow(r,power);
        dr = pow(r, power-1) * power * dr + 1;
        z = zr * (float3)(sin(theta) * cos(phi), sin(phi) * sin(theta), cos(theta));
        z+= p;
    }
    
    return (float2)(0.5f * log(r) * r / dr, iterations);

    
}
inline float3 calculate_normal(float3 p, float pow)
{
    const float3 small_step = (float3)(0.001f, 0.0f, 0.0f);

    float gradient_x = mapWorld(p + small_step.xyy, pow).x - mapWorld(p - small_step.xyy, pow).x;
    float gradient_y = mapWorld(p + small_step.yxy, pow).x - mapWorld(p - small_step.yxy, pow).x;
    float gradient_z = mapWorld(p + small_step.yyx, pow).x - mapWorld(p - small_step.yyx, pow).x;

    float3 normal = (float3)(gradient_x, gradient_y, gradient_z);

    return normalize(normal);
}

inline short3 raymarch(float3 ro, float3 rd, float pow){
    const float3 sun = normalize((float3)(-1,-2,0));
    const float3 colorAMix = (float3)(0.1f,0,0);
    const float3 colorBMix = (float3)(0,0,1);
    float totalDist = 0;
    int NUM_STEPS = 128;
    int totalSteps = 0;
    float MIN_DIST = 0.001f;
    float MAX_DIST = 1000;
    for(int i = 0; i<NUM_STEPS; i++){
        float3 currentPos =  ro + totalDist * rd;
        float2 distToClosest = mapWorld(currentPos,pow);
        
        if(distToClosest.x < MIN_DIST){

            float3 norm = calculate_normal(currentPos, pow);
            float colorA = clamp(dot(norm*0.5f+0.5f,-sun),0.0f,1.0f); //from https://github.com/SebLague/Ray-Marching/blob/master/Assets/Scripts/Fractal/Fractal.compute
            float colorB = clamp(distToClosest.y/16.0f, 0.0f, 1.0f);
            float3 col = clamp(colorA * colorAMix + colorB * colorBMix, 0.0f, 1.0f);
            col *= 255;
            

            return (short3)(col.x,col.y,col.z);
        }
        if(totalDist > MAX_DIST){
            break;
        }
        totalDist += distToClosest.x;
        totalSteps++;
    }
float3 col = totalSteps * normalize((float3)(0.25f,0,1.0f));
return (short3)(col.x,col.y,col.z);
}

__kernel void
sampleKernel(__global short *r,
             __global short *g,
             __global short *b,
             const float3 ro,
             const float2 angle,
             const float pow)
{

    int gid = get_global_id(0);
    float x = gid % 700;
    float y = gid / 700;
    //float3 ro = (float3)(0,0,-10);
    float z =x/350 - 1;
    float3 rd = (float3)(1, y/350-1, 1);
    rd.x = z * angle.x + angle.y;
    rd.z = z * angle.y - angle.x;
    rd = normalize(rd);
    short3 color = raymarch(ro, rd, pow);
    r[gid]=color.x;
    g[gid]=color.y;
    b[gid]=color.z;

    
    
}
