package comp5216.sydney.edu.au.grocerylist;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.List;

public class ViewItemsActivity extends AppCompatActivity {
    ListView itemsListView;
    List<String> itemsList;
    ArrayAdapter<String> itemsArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Use activity_view_items.xml as the layout
        setContentView(R.layout.activity_view_items);

        // Link Java objects to Android components
        itemsListView = (ListView) findViewById(R.id.itemsListView);

        // Create an adapter for the list view using Android's built-in item layout
        itemsList = getIntent().getStringArrayListExtra("item");
        itemsArrayAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, itemsList);

        // Connect the items ListView and the adapter
        itemsListView.setAdapter(itemsArrayAdapter);
    }
}