package sc.fiji;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.Convolver;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

public class HarrisCornerDetector {
    public static final float DEFAULT_ALPHA = 0.050f;
    public static final int DEFAULT_THRESHOLD = 20000;
    public static final double DEFAULT_DMIN = 10;
    public static final int DEFAULT_BORDER = 20;

    private float alpha = DEFAULT_ALPHA;
    private int threshold = DEFAULT_THRESHOLD;
    private double dmin = DEFAULT_DMIN;
    private int border = DEFAULT_BORDER;
    private boolean doCleanUp = true;

    // filter kernels (one-dim. part of separable 2D filters)
    private final float[] pfilt = {0.223755f, 0.552490f, 0.223755f};
    private final float[] dfilt = {0.453014f, 0.0f, -0.453014f};
    private final float[] bfilt = {0.01563f, 0.09375f, 0.234375f, 0.3125f, 0.234375f, 0.09375f, 0.01563f};
        // = {1,6,15,20,15,6,1}/64

    private ImageProcessor ipOrig;
    private FloatProcessor A;
    private FloatProcessor B;
    private FloatProcessor C;
    private FloatProcessor Q;
    private List<Corner> corners;

    public HarrisCornerDetector(ImageProcessor ip){
        this.ipOrig = ip;
    }

    public HarrisCornerDetector(ImageProcessor ip, float alpha, int threshold){
        this(ip);
        this.alpha = alpha;
        this.threshold = threshold;
    }

    public void setDmin (double dmin) {
        this.dmin = dmin;
    }

    public void setBorder (int border) {
        this.border = border;
    }

    public void setDoCleanup (boolean doCleanup) {
        this.doCleanUp = doCleanup;
    }

    public void findCorners(){
        makeDerivatives();
        makeCrf();	//corner response function (CRF)
        corners = collectCorners(border);
        if (doCleanUp) {
            corners = cleanupCorners(corners);
        }
    }

    private void makeDerivatives(){
        FloatProcessor Ix = (FloatProcessor) ipOrig.convertToFloat();
        FloatProcessor Iy = (FloatProcessor) ipOrig.convertToFloat();

        Ix = convolve1h(convolve1h(Ix,pfilt),dfilt);
        Iy = convolve1v(convolve1v(Iy,pfilt),dfilt);

        A = sqr((FloatProcessor) Ix.duplicate());
        A = convolve2(A,bfilt);

        B = sqr((FloatProcessor) Iy.duplicate());
        B = convolve2(B,bfilt);

        C = mult((FloatProcessor)Ix.duplicate(),Iy);
        C = convolve2(C,bfilt);
    }

    private void makeCrf() { //corner response function (CRF)
        int w = ipOrig.getWidth();
        int h = ipOrig.getHeight();
        Q = new FloatProcessor(w,h);
        float[] Apix = (float[]) A.getPixels();
        float[] Bpix = (float[]) B.getPixels();
        float[] Cpix = (float[]) C.getPixels();
        float[] Qpix = (float[]) Q.getPixels();
        for (int v=0; v<h; v++) {
            for (int u=0; u<w; u++) {
                int i = v*w+u;
                float a = Apix[i], b = Bpix[i], c = Cpix[i];
                float det = a*b-c*c;
                float trace = a+b;
                Qpix[i] = det - alpha * (trace * trace);
            }
        }
    }

    private List<Corner> collectCorners(int border) {
        List<Corner> cornerList = new Vector<Corner>(1000);
        int w = Q.getWidth();
        int h = Q.getHeight();
        float[] Qpix = (float[]) Q.getPixels();
        for (int v = border; v < h-border; v++){
            for (int u = border; u < w-border; u++) {
                float q = Qpix[v*w+u];
                if (q > threshold && isLocalMax(Q,u,v)) {
                    Corner c = new Corner(u,v,q);
                    cornerList.add(c);
                }
            }
        }
        Collections.sort(cornerList);	// sort corners by descending q-value
        return cornerList;
    }

    private List<Corner> cleanupCorners(List<Corner> corners){
        // corners are assumed to be sorted by descending q-value
        double dmin2 = dmin*dmin;
        Corner[] cornerArray = new Corner[corners.size()];
        cornerArray = corners.toArray(cornerArray);
        List<Corner> goodCorners = new Vector<Corner>(corners.size());
        for (int i=0; i<cornerArray.length; i++){
            if (cornerArray[i] != null){
                Corner c1 = cornerArray[i];
                goodCorners.add(c1);
                //delete all remaining corners close to c
                for (int j=i+1; j<cornerArray.length; j++){
                    if (cornerArray[j] != null){
                        Corner c2 = cornerArray[j];
                        if (c1.dist2(c2)<dmin2)
                            cornerArray[j] = null;   //delete corner c2
                    }
                }
            }
        }
        return goodCorners;
    }

    void printCornerPoints(List<Corner> crf){
        int i = 0;
        for (Corner ipt: crf){
            IJ.log((i++) + ": " + (int)ipt.q + " " + ipt.u + " " + ipt.v);
        }
    }

    public ImageProcessor showCornerPoints(ImageProcessor ip, int nmax) {
        ByteProcessor ipResult = ip.duplicate().convertToByteProcessor();
        //change background image contrast and brightness
        int[] lookupTable = new int[256];
        for (int i=0; i<256; i++){
            lookupTable[i] = 128 + (i/2);
        }
        ipResult.applyTable(lookupTable);
        //draw corners:
        int n = 0;
        for (Corner c: corners) {
            c.draw(ipResult);
            n = n + 1;
            if (nmax > 0 && n >= nmax)
                break;
        }
        return ipResult;
    }

    void showProcessor(ImageProcessor ip, String title){
        //ImageProcessor bip = ip.convertToByte(false);
        ImagePlus win = new ImagePlus(title,ip);
        win.show();
    }

    // utility methods for float processors

    private FloatProcessor convolve1h (FloatProcessor I, float[] h) {
        Convolver conv = new Convolver();
        conv.setNormalize(false);
        conv.convolve(I, h, 1, h.length);
        return I;
    }

    private FloatProcessor convolve1v (FloatProcessor I, float[] h) {
        Convolver conv = new Convolver();
        conv.setNormalize(false);
        conv.convolve(I, h, h.length, 1);
        return I;
    }

    private FloatProcessor convolve2 (FloatProcessor I, float[] h) {
        convolve1h(I,h);
        convolve1v(I,h);
        return I;
    }

    private FloatProcessor sqr (FloatProcessor I) {
        I.sqr();
        return I;
    }

    private FloatProcessor mult (FloatProcessor I1, FloatProcessor I2) {
        I1.copyBits(I2, 0, 0, Blitter.MULTIPLY);
        return I1;
    }


    private boolean isLocalMax (FloatProcessor fp, int u, int v) {
        int w = fp.getWidth();
        int h = fp.getHeight();
        if (u<=0 || u>=w-1 || v<=0 || v>=h-1)
            return false;
        else {
            float[] pix = (float[]) fp.getPixels();
            int i0 = (v-1)*w+u, i1 = v*w+u, i2 = (v+1)*w+u;
            float cp = pix[i1];
            return
                    cp >= pix[i0-1] && cp >= pix[i0] && cp >= pix[i0+1] &&
                            cp >= pix[i1-1] &&                  cp >= pix[i1+1] &&
                            cp >= pix[i2-1] && cp >= pix[i2] && cp >= pix[i2+1] ;
        }
    }

}

class Corner implements Comparable<Corner> {
    int u;
    int v;
    float q;

    Corner (int u, int v, float q) {
        this.u = u;
        this.v = v;
        this.q = q;
    }

    public int compareTo (Corner c2) {
        //used for sorting corners by corner strength q
        if (this.q > c2.q) return -1;
        if (this.q < c2.q) return 1;
        else return 0;
    }

    double dist2 (Corner c2){
        //returns the squared distance between this corner and corner c2
        int dx = this.u - c2.u;
        int dy = this.v - c2.v;
        return (dx*dx)+(dy*dy);
    }

    void draw(ImageProcessor ip){
        //draw this corner as a black cross
        int paintvalue = 0; //black
        int size = 2;
        ip.setValue(paintvalue);
        ip.drawLine(u-size,v,u+size,v);
        ip.drawLine(u,v-size,u,v+size);
    }
}