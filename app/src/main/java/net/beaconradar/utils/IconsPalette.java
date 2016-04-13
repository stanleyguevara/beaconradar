package net.beaconradar.utils;

import android.support.annotation.NonNull;

import net.beaconradar.R;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;

import javax.inject.Singleton;

@Singleton
public class IconsPalette {
    private static final Class<?> clazz = R.drawable.class;
    public static final LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
    static {
        //Standard
        map.put("ic_fingerprint", R.drawable.ic_fingerprint);
        map.put("ic_link_variant", R.drawable.ic_link_variant);
        map.put("ic_thermometer", R.drawable.ic_thermometer);
        map.put("ic_call_merge", R.drawable.ic_call_merge);
        map.put("ic_hexagon_outline", R.drawable.ic_hexagon_outline);
        map.put("ic_apple2_full", R.drawable.ic_apple2_full);
        //Numbers
        map.put("ic_numeric_0_box", R.drawable.ic_numeric_0_box);
        map.put("ic_numeric_1_box", R.drawable.ic_numeric_1_box);
        map.put("ic_numeric_2_box", R.drawable.ic_numeric_2_box);
        map.put("ic_numeric_3_box", R.drawable.ic_numeric_3_box);
        map.put("ic_numeric_4_box", R.drawable.ic_numeric_4_box);
        map.put("ic_numeric_5_box", R.drawable.ic_numeric_5_box);
        map.put("ic_numeric_6_box", R.drawable.ic_numeric_6_box);
        map.put("ic_numeric_7_box", R.drawable.ic_numeric_7_box);
        map.put("ic_numeric_8_box", R.drawable.ic_numeric_8_box);
        map.put("ic_numeric_9_box", R.drawable.ic_numeric_9_box);
        //Arrows
        map.put("ic_chevron_double_left", R.drawable.ic_chevron_double_left);
        map.put("ic_chevron_double_up", R.drawable.ic_chevron_double_up);
        map.put("ic_chevron_double_right", R.drawable.ic_chevron_double_right);
        map.put("ic_chevron_double_down", R.drawable.ic_chevron_double_down);
        map.put("ic_arrow_left_bold_circle_outline", R.drawable.ic_arrow_left_bold_circle_outline);
        map.put("ic_arrow_up_bold_circle_outline", R.drawable.ic_arrow_up_bold_circle_outline);
        map.put("ic_arrow_right_bold_circle_outline", R.drawable.ic_arrow_right_bold_circle_outline);
        map.put("ic_arrow_down_bold_circle_outline", R.drawable.ic_arrow_down_bold_circle_outline);
        map.put("ic_adjust", R.drawable.ic_adjust);
        //Misc
        map.put("ic_eddystone2", R.drawable.ic_eddystone2);
        map.put("ic_google_physical_web", R.drawable.ic_google_physical_web);
        map.put("ic_home", R.drawable.ic_home);
        map.put("ic_car", R.drawable.ic_car);
        map.put("ic_bike", R.drawable.ic_bike);
        map.put("ic_airplane", R.drawable.ic_airplane);
        map.put("ic_road_variant", R.drawable.ic_road_variant);
        map.put("ic_stairs", R.drawable.ic_stairs);
        map.put("ic_tie", R.drawable.ic_tie);
        map.put("ic_silverware", R.drawable.ic_silverware);
        map.put("ic_school", R.drawable.ic_school);
        map.put("ic_image_filter_hdr", R.drawable.ic_image_filter_hdr);
        map.put("ic_image_filter_drama", R.drawable.ic_image_filter_drama);
        map.put("ic_pine_tree", R.drawable.ic_pine_tree);
        map.put("ic_leaf", R.drawable.ic_leaf);
        map.put("ic_water", R.drawable.ic_water);
        map.put("ic_compass_outline", R.drawable.ic_compass_outline);
        map.put("ic_flag_outline", R.drawable.ic_flag_outline);
        map.put("ic_key_variant", R.drawable.ic_key_variant);
        map.put("ic_lightbulb_outline", R.drawable.ic_lightbulb_outline);
        map.put("ic_pulse", R.drawable.ic_pulse);
        map.put("ic_flash", R.drawable.ic_flash);
        map.put("ic_flask_outline", R.drawable.ic_flask_outline);
        map.put("ic_black_mesa", R.drawable.ic_black_mesa);
        map.put("ic_radioactive", R.drawable.ic_radioactive);
        map.put("ic_tag_text_outline", R.drawable.ic_tag_text_outline);
        map.put("ic_puzzle", R.drawable.ic_puzzle);
        map.put("ic_alarm", R.drawable.ic_alarm);
        map.put("ic_timer_sand", R.drawable.ic_timer_sand);
        map.put("ic_watch", R.drawable.ic_watch);
        map.put("ic_sunglasses", R.drawable.ic_sunglasses);
        map.put("ic_tshirt_crew", R.drawable.ic_tshirt_crew);
        map.put("ic_star", R.drawable.ic_star);
        map.put("ic_headphones", R.drawable.ic_headphones);
        map.put("ic_camera", R.drawable.ic_camera);
        map.put("ic_beer", R.drawable.ic_beer);
        map.put("ic_volume_high", R.drawable.ic_volume_high);
        map.put("ic_music_note", R.drawable.ic_music_note);
        map.put("ic_heart", R.drawable.ic_heart);
    }

    /**
     * Method to get Android Drawable id, using predefined static map. Recommended.
     * @param name Drawable name (e.g. "ic_some_icon").
     * @return Android Drawable Id, -1 if not found.
     */
    public static int getResId(@NonNull String name) {
        Integer resId = map.get(name);
        if(resId != null) return resId;
        else return R.drawable.ic_help_circle;
    }

    /**
     * Method to get Drawable id, using reflection. Not recommended.
     * @param name Drawable name (e.g. "ic_some_icon").
     * @return Android Drawable Id, -1 if not found.
     */
    @SuppressWarnings("unused")
    public static int getResIdReflection(@NonNull String name) {
        try {
            Field idField = clazz.getDeclaredField(name);
            return idField.getInt(idField);
        } catch (Exception e) {
            e.printStackTrace();
            return R.drawable.ic_help_circle;
        }
    }
}
