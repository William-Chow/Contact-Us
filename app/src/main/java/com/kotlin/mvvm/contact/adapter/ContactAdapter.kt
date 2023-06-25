package com.kotlin.mvvm.contact.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kotlin.mvvm.contact.R
import com.kotlin.mvvm.contact.model.Contact

internal class ContactAdapter(var context: Context) : RecyclerView.Adapter<ContactAdapter.ContactHolder>() {

    private var contactList = ArrayList<Contact>()

    var onItemClick: ((Contact, Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
        return ContactHolder(itemView)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ContactHolder, position: Int) {
        val contact = contactList[position]
        holder.name.text = contact.firstName + " " + contact.lastName
    }

    override fun getItemCount(): Int {
        return contactList.size
    }

    internal inner class ContactHolder(view: View) : RecyclerView.ViewHolder(view) {
        var name: TextView = view.findViewById(R.id.tvName)

        init {
            view.setOnClickListener {
                onItemClick?.invoke(contactList[adapterPosition], adapterPosition)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(contactList: List<Contact>) {
        this.contactList = contactList as ArrayList<Contact>
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun sortList() {
        this.contactList.reverse()
        notifyDataSetChanged()
    }
}