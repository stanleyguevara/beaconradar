package net.beaconradar.details;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.formatter.YAxisValueFormatter;
import net.beaconradar.utils.ZeroNorm;

public class ZeroFormatter implements YAxisValueFormatter {
    private ZeroNorm norm;
    private boolean mode;

    public ZeroFormatter(ZeroNorm norm, boolean mode) {
        this.norm = norm;
        this.mode = mode;
    }

    @Override
    public String getFormattedValue(float value, YAxis yAxis) {
        if(mode == DetailsView.MODE_DISTANCE) {
            float range = yAxis.mAxisRange;
            String result;
            if(range < 1.0f) {
                result = norm.getLabel(value, 2);
                if("-0.00".equals(result)) result = "0.00";     //Due to rounding bug in 3rd party library
            } else if (range < 10.0f) {
                result = norm.getLabel(value, 1);
                if("-0.0".equals(result)) result = "0.0";
            } else {
                result = norm.getLabel(value, 0);
                if("-0".equals(result)) result = "0";
            }
            return result;
        } else {
            return norm.getLabel(value, 0);
        }
    }
}
