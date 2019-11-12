package KochSnowflake;

import graphicslib3D.*;
import graphicslib3D.GLSLUtils.*;

import java.nio.*;
import javax.swing.*;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL2ES3.GL_COLOR;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.common.nio.Buffers;
import java.util. Random;

/**
 * CS-4163 Computer Graphics, Dr. Mauricio Papa
 * @author sirkevinicus
 * Sources: Wikipedia, https://www.calculatorsoup.com/calculators/geometry-plane/triangles-equilateral.php
 */

public class KochSnowflakePoly extends JFrame implements GLEventListener
{	
	private GLCanvas myCanvas;
	private int rendering_program;
	private int vao[] = new int[1];
	private int vbo[] = new int[2];
	private float cameraX, cameraY, cameraZ;
	private GLSLUtils util = new GLSLUtils();
	
	private int N; //Recursion level
	private float sideLength;
	
	private float[] vertex_positions=new float[3*2];//Three points, two coordinates
	
	public KochSnowflakePoly(float l, int r)
	{	
		//Making sure we get a GL4 context for the canvas
        GLProfile profile = GLProfile.get(GLProfile.GL4);
        GLCapabilities capabilities = new GLCapabilities(profile);
		myCanvas = new GLCanvas(capabilities);
 		//end GL4 context
		
		//initialize levels of recursion, sideLength of the fractal
		N = r;
		sideLength = l;
		
		for(int i = 0; i <= N; i++) {
			printCalculations(i);
		}
		
		//change the window size depending on the size of the triangle
		setTitle("Koch Snowflake");
		setSize(600,600);
		
		myCanvas.addGLEventListener(this);
		getContentPane().add(myCanvas);
		this.setVisible(true);
	}
	
	public static void main(String[] args) { 
		new KochSnowflakePoly(1.0f, 5);
	}

	public void display(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		
		//Define the triangle
		float[] v1 = new float[2];//Two coordinates
		float[] v2 = new float[2];//Two coordinates
		float[] v3 = new float[2];//Two coordinates
		
		//The first three vertices define the starting triangle
		//Equilateral triangle centered at the origin
		
		//Top vertex - x and y
		v1[0]=0; 
		v1[1]=sideLength*(float)Math.sqrt(3)/3;
		//Bottom left
		v2[0]=-0.5f*sideLength; 
		v2[1]=-(float)Math.sqrt(3)*sideLength/6;
		//Bottom right
		v3[0]=0.5f*sideLength; 
		v3[1]=-(float)Math.sqrt(3)*sideLength/6;
		//Done defining triangle
		
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glUseProgram(rendering_program);

		int mv_loc = gl.glGetUniformLocation(rendering_program, "mv_matrix");
		int proj_loc = gl.glGetUniformLocation(rendering_program, "proj_matrix");

		//what is this?
		
		//float aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		Matrix3D pMat = orthogonal(-1.5f,1.5f,1.5f,-1.5f,0.1f,1000.0f);

		Matrix3D vMat = new Matrix3D();
		vMat.translate(-cameraX, -cameraY, -cameraZ);
		//Just drawing 2D - not moving the object
		Matrix3D mMat = new Matrix3D();
		mMat.setToIdentity();

		Matrix3D mvMat = new Matrix3D();
		mvMat.concatenate(vMat);
		mvMat.concatenate(mMat);

		gl.glUniformMatrix4fv(mv_loc, 1, false, mvMat.getFloatValues(), 0);
		gl.glUniformMatrix4fv(proj_loc, 1, false, pMat.getFloatValues(), 0);

		//gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);//We are only passing two components
		gl.glEnableVertexAttribArray(0);

		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		
		//BACKGROUND STUFF
		float bkg[] = { 0.106f, 0.459f, 0.059f, 1.0f };
		FloatBuffer bkgBuffer = Buffers.newDirectFloatBuffer(bkg);
		gl.glClearBufferfv(GL_COLOR, 0, bkgBuffer);
		//

		processTriangle(v1,v2,v3,N);
	}

	public void init(GLAutoDrawable drawable)
	{	
		GL4 gl = (GL4) drawable.getGL();
		rendering_program = createShaderProgram();
		cameraX = 0.0f; cameraY = 0.0f; cameraZ = 3.0f;
		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);
		gl.glGenBuffers(vbo.length, vbo, 0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
	}

	private Matrix3D orthogonal(float left, float right, float top, float bottom, float near, float far)
	{
		Matrix3D r = new Matrix3D();
		r.setElementAt(0,0,2.0/(right-left));
		r.setElementAt(1,1,2.0/(top-bottom));
		r.setElementAt(2,2,1/(far-near));
		r.setElementAt(3,3,1.0f);
		r.setElementAt(0,3,-(right+left)/(right-left));
		r.setElementAt(1,3,-(top+bottom)/(top-bottom));
		r.setElementAt(2, 3, -near/(far-near));
		return r;
	}

	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
	public void dispose(GLAutoDrawable drawable) {}

	private int createShaderProgram()
	{	
		GL4 gl = (GL4) GLContext.getCurrentGL();

		String vshaderSource[] = util.readShaderSource("src/KochSnowflake/vert.shader.2d");
		String fshaderSource[] = util.readShaderSource("src/KochSnowflake/frag.shader.2d");

		int vShader = gl.glCreateShader(GL_VERTEX_SHADER);
		int fShader = gl.glCreateShader(GL_FRAGMENT_SHADER);

		gl.glShaderSource(vShader, vshaderSource.length, vshaderSource, null, 0);
		gl.glShaderSource(fShader, fshaderSource.length, fshaderSource, null, 0);

		gl.glCompileShader(vShader);
		gl.glCompileShader(fShader);

		int vfprogram = gl.glCreateProgram();
		gl.glAttachShader(vfprogram, vShader);
		gl.glAttachShader(vfprogram, fShader);
		gl.glLinkProgram(vfprogram);
		return vfprogram;
	}
	
	private void printCalculations(int n) {
		
		int segs = 3 * (int)Math.pow(4,n);
		float firstArea = ((float)Math.sqrt(3)/4) * (float)(Math.pow(sideLength, 2));
		float segLength = sideLength / (float)Math.pow(3,n);
		float perimeter = segLength * segs;
		
		System.out.println("=-=-=-=-=-=-=-=-=-=-=");
		System.out.println("N: " + n);
		System.out.println("SIDE LENGTH: " + segLength);
		System.out.println("#SEGMENTS: " + segs);
		System.out.println("PERIMETER: " + perimeter);
		System.out.println("AREA: " + (firstArea / 5) * (8 - (3 * Math.pow((float) 4 / 9, n)))); //found this equation on Wikipedia
		System.out.println("=-=-=-=-=-=-=-=-=-=-=");
		
	}
 
	//Processing triangles
	private void processTriangle(float[] v1, float[] v2, float[] v3, int n) {
		calcTrianglePoints(v1, v2, n);
		calcTrianglePoints(v2, v3, n);
		calcTrianglePoints(v3, v1, n);
	}
	
	private void calcTrianglePoints(float[] p1, float[] p2, int n) {
		if(n>0) {
			float dist_x = p2[0]-p1[0];
			float dist_y = p2[1]-p1[1];
	
			//Find new points
			float[] z1 = {	p1[0] + ((float) 1 / 3) * dist_x,
							p1[1] + ((float) 1 / 3) * dist_y
			};
			
			float[] z2 = {	p1[0] + ((float) 2 / 3) * dist_x,
							p1[1] + ((float) 2 / 3) * dist_y
			};
			
			//Find the new point
			//The halfway point is (p1[i] + p2[i]) / 2 (average of the two points)
			float[] z3 = {	(p1[0] + p2[0]) * ((float) 1/2) + ((float) Math.sqrt(3) / 6) * (dist_y),
							(p1[1] + p2[1]) * ((float) 1/2) - ((float) Math.sqrt(3) / 6) * (dist_x)
			};
		
			calcTrianglePoints(p1, z1, n-1);
			calcTrianglePoints(z1, z3, n-1);
			calcTrianglePoints(z3, z2, n-1);
			calcTrianglePoints(z2, p2, n-1);
		}
		else {
			draw(p1, p2);
		}
	}

	private void draw(float [] v1, float[] v2) {

		GL4 gl = (GL4) GLContext.getCurrentGL();
		//Store points in backing store
		vertex_positions[0]=v1[0];
		vertex_positions[1]=v1[1];
		vertex_positions[2]=v2[0];
		vertex_positions[3]=v2[1];

		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(vertex_positions);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit()*4, vertBuf, GL_STATIC_DRAW);

		//Draw now
		gl.glDrawArrays(GL_LINES, 0, 2);

	}

}