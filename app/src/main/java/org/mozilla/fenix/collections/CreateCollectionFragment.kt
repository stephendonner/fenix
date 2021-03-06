package org.mozilla.fenix.collections

/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_create_collection.view.*
import org.mozilla.fenix.R
import org.mozilla.fenix.components.FenixSnackbar
import org.mozilla.fenix.ext.getRootView
import org.mozilla.fenix.mvi.ActionBusFactory
import org.mozilla.fenix.mvi.getAutoDisposeObservable
import org.mozilla.fenix.mvi.getManagedEmitter

class CreateCollectionFragment : DialogFragment() {
    private lateinit var collectionCreationComponent: CollectionCreationComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
        setStyle(STYLE_NO_TITLE, R.style.CreateCollectionDialogStyle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create_collection, container, false)

        val viewModel = activity?.run {
            ViewModelProviders.of(this).get(CreateCollectionViewModel::class.java)
        }
        val tabs = viewModel!!.tabs
        val selectedTabs = viewModel.selectedTabs
        val step = viewModel.saveCollectionStep

        collectionCreationComponent = CollectionCreationComponent(
            view.create_collection_wrapper,
            ActionBusFactory.get(this),
            CollectionCreationState(
                tabs = tabs,
                selectedTabs = selectedTabs,
                saveCollectionStep = step
            )
        )
        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnKeyListener { _, keyCode, event ->
            (collectionCreationComponent.uiView as CollectionCreationUIView).onKey(keyCode, event)
        }
        return dialog
    }

    override fun onResume() {
        super.onResume()
        (collectionCreationComponent.uiView as CollectionCreationUIView).onResumed()
        subscribeToActions()
    }

    private fun subscribeToActions() {
        getAutoDisposeObservable<CollectionCreationAction>().subscribe {
            when (it) {
                is CollectionCreationAction.Close -> dismiss()
                is CollectionCreationAction.SaveTabsToCollection -> {
                    getManagedEmitter<CollectionCreationChange>()
                        .onNext(CollectionCreationChange.StepChanged(SaveCollectionStep.SelectCollection))
                }
                is CollectionCreationAction.AddTabToSelection -> getManagedEmitter<CollectionCreationChange>()
                    .onNext(CollectionCreationChange.TabAdded(it.tab))
                is CollectionCreationAction.RemoveTabFromSelection -> getManagedEmitter<CollectionCreationChange>()
                    .onNext(CollectionCreationChange.TabRemoved(it.tab))
                is CollectionCreationAction.SelectAllTapped -> getManagedEmitter<CollectionCreationChange>()
                    .onNext(CollectionCreationChange.AddAllTabs)
                is CollectionCreationAction.AddNewCollection -> getManagedEmitter<CollectionCreationChange>().onNext(
                    CollectionCreationChange.StepChanged(SaveCollectionStep.NameCollection)
                )
                is CollectionCreationAction.BackPressed -> handleBackPress(backPressFrom = it.backPressFrom)
                is CollectionCreationAction.SaveCollectionName -> {
                    showSavedSnackbar(it.tabs.size)
                    dismiss()
                }
            }
        }
    }

    private fun showSavedSnackbar(tabSize: Int) {
        context?.let { context: Context ->
            val rootView = context.getRootView()
            rootView?.let { view: View ->
                val string =
                    if (tabSize > 1) context.getString(R.string.create_collection_tabs_saved) else
                        context.getString(R.string.create_collection_tab_saved)
                FenixSnackbar.make(view, Snackbar.LENGTH_LONG).setText(string)
                    .show()
            }
        }
    }

    private fun handleBackPress(backPressFrom: SaveCollectionStep) {
        when (backPressFrom) {
            SaveCollectionStep.SelectTabs -> dismiss()
            SaveCollectionStep.SelectCollection -> getManagedEmitter<CollectionCreationChange>().onNext(
                CollectionCreationChange.StepChanged(SaveCollectionStep.SelectTabs)
            )
            SaveCollectionStep.NameCollection -> {
                getManagedEmitter<CollectionCreationChange>().onNext(
                    CollectionCreationChange.StepChanged(SaveCollectionStep.SelectCollection)
                )
            }
        }
    }

    companion object {
        const val createCollectionTag = "createCollection"
    }
}
