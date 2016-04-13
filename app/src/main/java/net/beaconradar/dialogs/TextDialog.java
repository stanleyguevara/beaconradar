package net.beaconradar.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialogFragment;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import net.beaconradar.R;
import net.beaconradar.utils.CircleImageView;

public class TextDialog extends AppCompatDialogFragment {

    public interface NameChangedListener {
        void onNameChanged(String name);
    }

    private NameChangedListener listener;

    public static TextDialog newInstance(String name, int color, int icon) {
        TextDialog instance = new TextDialog();
        Bundle args = new Bundle();
        args.putString("name", name);
        args.putInt("color", color);
        args.putInt("icon", icon);
        instance.setArguments(args);
        return instance;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String name = getArguments().getString("name");
        Dialog dialog = new MaterialDialog.Builder(getActivity())
                .title("Edit name")
                .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME | InputType.TYPE_TEXT_FLAG_CAP_WORDS)
                .positiveText(R.string.positive_button)
                .alwaysCallInputCallback()
                .inputRange(1, 128)
                .input(getString(R.string.name_hint), name, false, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                        if (input.toString().length() < 1) {
                            //dialog.setContent(R.string.too_short);
                            dialog.getActionButton(DialogAction.POSITIVE).setEnabled(false);
                        } else {
                            if (input.toString().length() > 128) {
                                //dialog.setContent(R.string.too_long);
                                dialog.getActionButton(DialogAction.POSITIVE).setEnabled(false);
                            } else {
                                //dialog.setContent(R.string.whatever_you_like);
                                dialog.getActionButton(DialogAction.POSITIVE).setEnabled(true);
                            }
                        }
                    }
                })
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction action) {
                        listener.onNameChanged(dialog.getInputEditText().getText().toString());
                    }
                }).build();
        MaterialDialog md = (MaterialDialog) dialog;
        final EditText editText = md.getInputEditText();
        if(editText != null) editText.post(new Runnable() {
            @Override
            public void run() {
                editText.selectAll();
            }
        });
        LinearLayout titleFrame = (LinearLayout) dialog.findViewById(R.id.titleFrame);
        //Hide old title
        View oldIcon = dialog.findViewById(R.id.icon);
        View oldTitle = dialog.findViewById(R.id.title);
        oldIcon.setVisibility(View.GONE);
        oldTitle.setVisibility(View.GONE);
        //Inflate new
        View.inflate(getContext(), R.layout.beacon_identifier_small, titleFrame);
        CircleImageView icon = (CircleImageView) titleFrame.findViewById(R.id.beacon_icon);
        TextView text = (TextView) titleFrame.findViewById(R.id.beacon_title);
        titleFrame.setGravity(Gravity.CENTER);
        text.setText(name);
        icon.setCircleColor(getArguments().getInt("color"));
        icon.setImageResource(getArguments().getInt("icon"));

        return dialog;
    }

    public void setChangedListener(NameChangedListener listener) {
        this.listener = listener;
    }

    @Nullable
    public static TextDialog findVisible(@NonNull AppCompatActivity context, String tag) {
        Fragment frag = context.getSupportFragmentManager().findFragmentByTag(tag);
        return (frag != null && frag instanceof TextDialog) ? (TextDialog) frag : null;
    }
}
