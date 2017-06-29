package android.lucas.com.mapstep;

/**
 * Created by lucas on 04/04/17.
 */

public class LastLocationUpdater {

    public static final double STEP_DISTANCE = 0.60; // tamanho médio de um passo segundo Sistema Internacional de Medidas

    public static Point calcNextLocation(double theta,    // angulo
                                  double distance, // distancia percorrida
                                  Point init       // ponto inicial
    ) {

        theta = convert(theta);

        double deltaX = distance * Math.cos(Math.toRadians(theta));
        double deltaY = distance * Math.sin(Math.toRadians(theta));

        double deltaLon = deltaX / (111320*Math.cos(Math.toRadians(init.lat)));
        double deltaLat = deltaY / (110540);

        Point finalPosition = new Point();
        finalPosition.lat = init.lat + deltaLat;
        finalPosition.lon = init.lon + deltaLon;

        return finalPosition;                       // ponto final

    }

    public static double convert(double theta) { // conversao de sistema cartesiano
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