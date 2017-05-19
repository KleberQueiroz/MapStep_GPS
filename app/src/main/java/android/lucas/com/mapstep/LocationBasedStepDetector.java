package android.lucas.com.mapstep;

/**
 * Created by lucas on 04/04/17.
 */

public class LocationBasedStepDetector {

    public static final double STEP_DISTANCE = 0.60; // tamanho médio de um passo segundo Sistema Internacional de Medidas

    private double deltaLon;
    private double deltaLat;

    private double deltaX;
    private double deltaY; // meters

    public Point calcFinalPosition(double theta,    // angulo
                                   double distance, // distancia percorrida
                                   Point init       // ponto inicial
    )
    {
        theta = convert(theta);

        deltaX = distance * Math.cos(Math.toRadians(theta));
        deltaY = distance * Math.sin(Math.toRadians(theta));

        deltaLon = deltaX / (111320*Math.cos(Math.toRadians(init.lat)));
        deltaLat = deltaY / (110540);

        Point finalPosition = new Point();
        finalPosition.lat = init.lat + deltaLat;
        finalPosition.lon = init.lon + deltaLon;

        return finalPosition;                       // ponto final
    }

    public double convert(double theta) { // conversao de sistema cartesiano
        return -theta + 90;
    }

}

class Point {
    double lon;
    double lat;

    Point(){}

    Point(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }
}