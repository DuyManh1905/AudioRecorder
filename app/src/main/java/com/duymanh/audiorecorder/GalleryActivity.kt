package com.duymanh.audiorecorder

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.duymanh.audiorecorder.databinding.ActivityGalleryBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class GalleryActivity : AppCompatActivity(), OnItemClickListener {

    private lateinit var binding: ActivityGalleryBinding

    private lateinit var records: ArrayList<AudioRecord>
    private lateinit var mAdapter: Adapter
    private lateinit var db: AppDatabase

    private var allCheck = false

    private lateinit var toolbar: MaterialToolbar
    private lateinit var editBar: View
    private lateinit var btnClose: ImageButton
    private lateinit var btnSelectAll: ImageButton

    private lateinit var searchInput: TextInputEditText
    private lateinit var bottomSheet: LinearLayout
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    private lateinit var btnRename: ImageButton
    private lateinit var btnDelete: ImageButton

    private lateinit var tvRename: TextView
    private lateinit var tvDelete: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        btnRename = findViewById(R.id.btnEdit)
        btnDelete = findViewById(R.id.btnDelete)
        tvRename = findViewById(R.id.tvEdit)
        tvDelete = findViewById(R.id.tvDelete)

        editBar = findViewById(R.id.editBar)
        btnClose = findViewById(R.id.btnClose)
        btnSelectAll = findViewById(R.id.btnSelectAll)

        bottomSheet = findViewById(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN


        records = ArrayList()
        mAdapter = Adapter(records,this)

        db = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "audioRecords"
        ).build()

        binding.recyclerView.apply {
            adapter = mAdapter
            layoutManager = LinearLayoutManager(context)
        }
        fetchAll()

        searchInput = findViewById(R.id.search_input)
        searchInput.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                var query = s.toString()
                searchDatabase(query)
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        btnClose.setOnClickListener{
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)

            editBar.visibility = View.GONE
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

            records.map {
                it.isChecked = false
                mAdapter.setEditMode(false)
            }
        }

        btnSelectAll.setOnClickListener{
            allCheck = !allCheck
            records.map {
                it.isChecked = allCheck
                mAdapter.notifyDataSetChanged()

                if(allCheck){
                    disableRename()
                    enableDelete()
                }
                else{
                    disableRename()
                    disableDelete()
                }
            }
        }

        btnDelete.setOnClickListener{
            var builder = AlertDialog.Builder(this)
            builder.setTitle("Delete record?")
            val nbRecords = records.count{
                it.isChecked
            }
            builder.setMessage("Are you sure you want to delete $nbRecords record(s) ?")
            builder.setPositiveButton("Delete"){_, _, ->
                val toDelete = records.filter {
                    it.isChecked
                }.toTypedArray()
                GlobalScope.launch {
                    db.audioRecordDao().delete(toDelete)

                    runOnUiThread {
                        records.removeAll(toDelete)
                        mAdapter.notifyDataSetChanged()
                    }
                }
            }

            builder.setNegativeButton("Cancel"){_, _, ->

            }
        }
    }

    private fun disableRename (){
        btnRename.isClickable = false
        btnRename.backgroundTintList = ResourcesCompat.getColorStateList(resources, R.color.grayDarkDisable, theme)
        tvRename.setTextColor(ResourcesCompat.getColorStateList(resources, R.color.grayDarkDisable, theme))
    }

    private fun disableDelete (){
        btnDelete.isClickable = false
        btnDelete.backgroundTintList = ResourcesCompat.getColorStateList(resources, R.color.grayDarkDisable, theme)
        tvDelete.setTextColor(ResourcesCompat.getColorStateList(resources, R.color.grayDarkDisable, theme))
    }

    private fun enableRename (){
        btnRename.isClickable = true
        btnRename.backgroundTintList = ResourcesCompat.getColorStateList(resources, R.color.grayDark, theme)
        tvRename.setTextColor(ResourcesCompat.getColorStateList(resources, R.color.grayDark, theme))
    }

    private fun enableDelete (){
        btnDelete.isClickable = true
        btnDelete.backgroundTintList = ResourcesCompat.getColorStateList(resources, R.color.grayDark, theme)
        tvDelete.setTextColor(ResourcesCompat.getColorStateList(resources, R.color.grayDark, theme))
    }

    private fun searchDatabase(query: String) {
        GlobalScope.launch {
            records.clear()
            var queryResult = db.audioRecordDao().searchDatabase("%$query%")
            records.addAll(queryResult)
            runOnUiThread {
                mAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun fetchAll(){
        GlobalScope.launch {
            records.clear()
            var queryResult = db.audioRecordDao().getAll()
            records.addAll(queryResult)

            runOnUiThread {
                mAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onItemClickListener(position: Int) {
        var audioRecord = records[position]

        if(mAdapter.isEditMode()){
            records[position].isChecked = !records[position].isChecked
            mAdapter.notifyItemChanged(position)

            var nbSelected = records.count{
                it.isChecked
            }
            when(nbSelected){
                0 -> {
                    disableRename()
                    disableDelete()
                }
                1 -> {
                    enableDelete()
                    enableRename()
                }
                else -> {
                    disableRename()
                    enableDelete()
                }
            }
        }
        else{
            var intent = Intent(this,AudioPlayerActivity::class.java)
            intent.putExtra("filepath",audioRecord.filePath)
            intent.putExtra("filename",audioRecord.fileName)
            startActivity(intent)
        }
    }

    override fun onItemLongClickListener(position: Int) {
        mAdapter.setEditMode(true)
        records[position].isChecked = !records[position].isChecked
        mAdapter.notifyItemChanged(position)

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        if(mAdapter.isEditMode() && editBar.visibility == View.GONE){
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            supportActionBar?.setDisplayShowHomeEnabled(false)

            editBar.visibility = View.VISIBLE

            enableDelete()
            enableRename()
        }
    }
}