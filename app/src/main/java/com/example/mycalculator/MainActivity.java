package com.example.mycalculator;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

public class MainActivity extends AppCompatActivity {

    private EditText editText;
    private ListView listView;
    private String originalExpression = "";
    private ArrayAdapter<String> adapter;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = findViewById(R.id.editText);
        listView = findViewById(R.id.listView);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(adapter);

        sharedPreferences = getPreferences(MODE_PRIVATE);
        String savedItems = sharedPreferences.getString("listItems", "");

        if (!savedItems.isEmpty()) {
            String[] items = savedItems.split(",");
            adapter.addAll(items);
            adapter.notifyDataSetChanged();
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String clickedItem = adapter.getItem(position);
                editText.setText(clickedItem);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Save the items to SharedPreferences when the app is paused
        StringBuilder itemsStringBuilder = new StringBuilder();
        for (int i = 0; i < adapter.getCount(); i++) {
            itemsStringBuilder.append(adapter.getItem(i));
            if (i < adapter.getCount() - 1) {
                itemsStringBuilder.append(",");
            }
        }
        sharedPreferences.edit().putString("listItems", itemsStringBuilder.toString()).apply();
    }

    public void onButtonClick(View view) {
        String buttonText = ((Button) view).getText().toString();
        String editTextContent = editText.getText().toString();

        if (editTextContent.equals("Error")) {
            editText.setText("");
        }

        if (editTextContent.length() >= 2 && editTextContent.endsWith("+0")) {
            if (buttonText.matches("[0-9]")) {
                return;
            }
        }

        switch (buttonText) {
            case "=":
                if (!editTextContent.isEmpty()) {
                    try {
                        double result = evaluateExpression(editTextContent);
                        String formattedResult = formatResult(result);
                        String originalExpressionText = editText.getText().toString();

                        adapter.add(originalExpressionText);

                        adapter.notifyDataSetChanged();

                        editText.setText(formattedResult);

                        originalExpression = editTextContent;
                    } catch (Exception e) {
                        editText.setText("Error");
                    }
                }
                break;
            case "%":
                if (!editTextContent.isEmpty()) {
                    handlePercentage();
                }
                break;
            case "C":
                editText.setText("");
                break;
            case "Delete":
                if (!editTextContent.isEmpty()) {
                    deleteLastCharacter();
                }
                break;
            case ".":
                handleDecimalButton();
                break;
            case "0":
                handleZeroButton();
                break;
            case "Erase":
                adapter.clear();
                adapter.notifyDataSetChanged();
                break;
            case "+":
            case "-":
            case "*":
            case "/":
                handleOperator(buttonText);
                break;
            default:
                if (!editTextContent.isEmpty() || buttonText.matches("[1-9]")) {
                    if (editTextContent.equals("0") && !buttonText.equals(".")) {
                        editText.setText(buttonText);
                    } else {
                        editText.append(buttonText);
                    }
                }
                break;
        }
    }

    private void handleDecimalButton() {
        String editTextContent = editText.getText().toString();

        if (editTextContent.isEmpty() || endsWithOperator(editTextContent)) {
            editText.append("0.");
        } else {
            String[] values = editTextContent.split("[-+*/]");
            String lastValue = values[values.length - 1];

            if (!lastValue.contains(".")) {
                editText.append(".");
            }
        }
    }

    private void handleZeroButton() {
        String editTextContent = editText.getText().toString();

        if (editTextContent.isEmpty()) {
            editText.setText("0");
        } else if (!endsWithOperator(editTextContent) && !editTextContent.equals("0") && !editTextContent.matches("\\d*\\.\\d*")) {
            // Do not allow a leading zero unless it's a decimal number (e.g., 0.25)
            editText.append("0");
        } else if (editTextContent.endsWith("0") && !editTextContent.contains(".")) {
            editText.append(".");
        } else {
            editText.append("0");
        }
    }

    private void handleOperator(String operator) {
        String editTextContent = editText.getText().toString();

        if (!editTextContent.isEmpty()) {
            char lastChar = editTextContent.charAt(editTextContent.length() - 1);

            if (isOperator(lastChar)) {
                String[] values = editTextContent.split("[-+*/]");
                String lastValue = values[values.length - 1];

                if (lastValue.startsWith("0") && !lastValue.equals("0.")) {
                    if (lastValue.length() == 1) {
                        editTextContent = editTextContent.substring(0, editTextContent.length() - 1) + operator;
                    } else {
                        editTextContent = editTextContent.substring(0, editTextContent.length() - lastValue.length()) +
                                lastValue.substring(0, 1) + operator;
                    }
                } else {
                    editTextContent = editTextContent.substring(0, editTextContent.length() - 1) + operator;
                }
            } else {
                editText.append(operator);
            }
        }
    }

    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/';
    }

    private boolean endsWithOperator(String text) {
        return text.endsWith("+") || text.endsWith("-") || text.endsWith("*") || text.endsWith("/");
    }

    private double evaluateExpression(String expression) {
        Expression exp = new ExpressionBuilder(expression)
                .build();
        return exp.evaluate();
    }

    private void deleteLastCharacter() {
        int length = editText.length();
        if (length > 0) {
            editText.getText().delete(length - 1, length);
        }
    }

    private String formatResult(double result) {
        if (result == (long) result) {
            return String.format("%d", (long) result);
        } else {
            return String.valueOf(result);
        }
    }

    private void handlePercentage() {
        String editTextContent = editText.getText().toString();

        int lastOperatorPosition = Math.max(
                editTextContent.lastIndexOf("+"),
                Math.max(
                        editTextContent.lastIndexOf("-"),
                        Math.max(
                                editTextContent.lastIndexOf("*"),
                                editTextContent.lastIndexOf("/")
                        )
                )
        );

        if (lastOperatorPosition != -1) {
            String lastNumber = editTextContent.substring(lastOperatorPosition + 1);

            if (!lastNumber.isEmpty()) {
                double percentageValue = Double.parseDouble(lastNumber) / 100.0;

                editTextContent = editTextContent.substring(0, lastOperatorPosition + 1) + String.valueOf(percentageValue);

                editText.setText(editTextContent);
            }
        } else {
            double percentageValue = Double.parseDouble(editTextContent) / 100.0;

            editText.setText(String.valueOf(percentageValue));
        }
    }
}