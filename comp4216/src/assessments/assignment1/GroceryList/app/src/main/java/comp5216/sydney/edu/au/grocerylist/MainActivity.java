package comp5216.sydney.edu.au.grocerylist;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements DatePickerFragment.DatePickerFragmentListener {
    ListView datesListView;
    List<String> datesList;
    ArrayAdapter<String> datesArrayAdapter;
    Map<String, List<String>> dateToItemsList;
    EditText newItemNameEditText;
    String date;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Use "activity_main.xml" as the layout
        setContentView(R.layout.activity_main);

        // Link Java objects to Android components
        datesListView = (ListView) findViewById(R.id.datesListView);
        newItemNameEditText = (EditText) findViewById(R.id.newItemNameEditText);

        // Create a List of dates
        datesList = new ArrayList<String>();

        dateToItemsList = new HashMap<String, List<String>>();

        // Set the default date to now
        DateFormat dateFormat = DateFormat.getDateInstance();
        date = dateFormat.format(Calendar.getInstance().getTime());

        // Create an adapter for the dates list view using Android's built-in item layout
        datesArrayAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, datesList);

        // Link the list view and the array adapter
        datesListView.setAdapter(datesArrayAdapter);

        // Setup list view listeners
        setUpDatesListViewListener();
    }

    public void onAddItemClick(View view) {
        String newItemNameString = newItemNameEditText.getText().toString();
        if (newItemNameString.length() > 0) {
            if (!dateToItemsList.containsKey(date)) {
                dateToItemsList.put(date, new ArrayList<String>());
                datesArrayAdapter.add(date);
            }
            dateToItemsList.get(date).add(newItemNameString);
            newItemNameEditText.setText("");
        }
    }

    public void onPickADateClick(View v) {
        DialogFragment datePickerFragment = new DatePickerFragment();
        datePickerFragment.show(getSupportFragmentManager(), "datePicker");
    }

    public void setSelectedDate(String date) {
        this.date = date;
    }

    private void setUpDatesListViewListener() {
        // Register a request to start an activity for result and register the result callback
        ActivityResultLauncher<Intent> mLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) { // Extract name value from result extras
                    String editedItem = result.getData().getExtras().getString("item");
                    int position = result.getData().getIntExtra("position", -1);
                    datesList.set(position, editedItem);
                    Log.i("Updated item in list ", editedItem + ", position: " + position);

                    // Make a standard toast that just contains text
                    Toast.makeText(getApplicationContext(),
                            "Updated: " + editedItem,
                            Toast.LENGTH_SHORT).show();
                    datesArrayAdapter.notifyDataSetChanged();
                }
            }
        );

        datesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String updateItem = (String) datesArrayAdapter.getItem(position);
                Log.i("MainActivity", "Clicked item " + position + ": " +
                        updateItem.toString());

                Intent intent = new Intent(MainActivity.this,
                        ViewItemsActivity.class);

                // Put "extras" into the bundle for access in the "View Items Activity"
                intent.putStringArrayListExtra("item",
                        (ArrayList<String>) dateToItemsList.get(updateItem.toString()));

                mLauncher.launch(intent);
                datesArrayAdapter.notifyDataSetChanged();
            }
        });
    }
}
