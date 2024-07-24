package comp5216.sydney.edu.au.grocerylist;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.DatePicker;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.text.DateFormat;
import java.util.Calendar;

public class DatePickerFragment extends DialogFragment
        implements DatePickerDialog.OnDateSetListener {
    Calendar calendar = Calendar.getInstance();
    DatePickerFragmentListener listener;
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current date as the default date in the picker
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Create a new instance of DatePickerDialog and return it
        return new DatePickerDialog(requireContext(), this, year, month, day);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

        DateFormat dateFormat = DateFormat.getDateInstance();
        listener.setSelectedDate(dateFormat.format(calendar.getTime()));
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        listener = (DatePickerFragmentListener) activity;
    }

    public interface DatePickerFragmentListener {
        void setSelectedDate(String date);
    }
}