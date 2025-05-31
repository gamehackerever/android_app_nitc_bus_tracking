package com.nitc.nitcbustracker

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.nitc.nitcbustracker.RegisterActivity
import com.nitc.nitcbustracker.data.model.Notice
import com.nitc.nitcbustracker.data.model.RegisterRequest
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.sql.Timestamp
import java.time.Instant

class NewNoticeFragment : Fragment() {
    private lateinit var nameEditText: EditText
    private lateinit var toWhomSpinner: Spinner
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var pushNotificationCheckBox: CheckBox

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_new_notice, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        nameEditText = view.findViewById(R.id.editName)
        toWhomSpinner = view.findViewById(R.id.spinnerToWhom)
        messageEditText = view.findViewById(R.id.editTextMessage)
        sendButton = view.findViewById(R.id.buttonSend)
        pushNotificationCheckBox = view.findViewById(R.id.checkboxNotifs)


        sendButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val to_whom = toWhomSpinner.selectedItem.toString()
            val message = messageEditText.text.toString().trim()
            val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
            isoFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val timestamp = isoFormat.format(java.util.Date())

            if (name.isEmpty() || message.isEmpty()) {
                Toast.makeText(requireActivity(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = Notice(name, to_whom, message, timestamp)

            if (pushNotificationCheckBox.isChecked) {
                val call = RetrofitClient.api.sendNotification(request)

                call.enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            Toast.makeText(
                                requireActivity(),
                                "Notification sent",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                requireActivity(),
                                "Failed: ${response.code()}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Toast.makeText(requireActivity(), "Error: ${t.message}", Toast.LENGTH_LONG)
                            .show()
                    }
                })
            }

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.api.updateNotices(request)
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(requireActivity(), response.body()?.message ?: "Successfully Registered", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireActivity(), response.body()?.message ?: "Server Error", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireActivity(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            Toast.makeText(requireActivity(), "Notice Sent:\nTo: $to_whom", Toast.LENGTH_LONG).show()

        }

    }
}