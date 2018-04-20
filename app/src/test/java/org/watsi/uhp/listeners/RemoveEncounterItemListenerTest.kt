package org.watsi.uhp.listeners

import android.view.View

import org.junit.Test
import org.mockito.Mockito.*
import org.watsi.domain.entities.Encounter
import org.watsi.domain.entities.EncounterItem
import org.watsi.uhp.adapters.EncounterItemAdapter

class RemoveEncounterItemListenerTest {

    val mockView = mock(View::class.java)
    val mockEncounter = mock(Encounter::class.java)
    val mockEncounterItem = mock(EncounterItem::class.java)
    val mockEncounterItemAdapter = mock(EncounterItemAdapter::class.java)

    @Test
    fun onClick() {
        val listener = RemoveEncounterItemListener(
            mockEncounter, mockEncounterItem, mockEncounterItemAdapter)

        listener.onClick(mockView)

        verify(mockEncounterItemAdapter, times(1)).remove(mockEncounterItem)
        verify(mockEncounter, times(1)).removeEncounterItem(mockEncounterItem)
    }
}
