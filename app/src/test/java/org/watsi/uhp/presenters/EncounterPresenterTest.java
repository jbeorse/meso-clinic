package org.watsi.uhp.presenters;

import android.view.View;
import android.widget.Spinner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.watsi.uhp.R;
import org.watsi.uhp.adapters.EncounterItemAdapter;
import org.watsi.uhp.database.BillableDao;
import org.watsi.uhp.models.Billable;
import org.watsi.uhp.models.Encounter;
import org.watsi.uhp.models.EncounterItem;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;


@RunWith(PowerMockRunner.class)
@PrepareForTest(BillableDao.class)
public class EncounterPresenterTest {

    private EncounterPresenter encounterPresenter;
    private Encounter encounter;

    @Mock
    View view;

    @Mock
    Spinner spinner;

    @Mock
    EncounterItemAdapter encounterItemAdapter;

    @Before
    public void setup() {
        initMocks(this);
        mockStatic(BillableDao.class);
        encounter = new Encounter();
        encounterPresenter = new EncounterPresenter(encounter, view, encounterItemAdapter);

        Date occurredAt = Calendar.getInstance().getTime();
        encounter.setOccurredAt(occurredAt);
    }

    @Test
    public void getCategorySpinner() throws Exception {
        when(view.findViewById(R.id.category_spinner)).thenReturn(spinner);

        assertEquals(encounterPresenter.getCategorySpinner(), spinner);
    }

    @Test
    public void addToEncounterItemList() throws Exception {
        Billable billable = mock(Billable.class);
        encounterPresenter.addToEncounterItemList(billable);

        assertNotNull(encounterPresenter.mEncounter.getEncounterItems());
        EncounterItem encounterItem = encounterPresenter.mEncounter.getEncounterItems().get(0);
        assertNotNull(encounterItem);

        assertEquals(encounterItem.getBillable(), billable);
        verify(encounterItemAdapter, times(1)).add(encounterItem);
    }

    @Test
    public void newDateLinkText() throws Exception {
        assertThat(encounterPresenter.newDateLinkText(encounter), containsString(encounterPresenter.dateFormatter(encounter.getOccurredAt())));
    }

    @Test
    public void getCategoriesList() throws Exception {
        List<String> categoriesList = encounterPresenter.getCategoriesList("foo");

        assertEquals(categoriesList.toString(), "[foo, DRUG, SERVICE, LAB, SUPPLY, VACCINE]");
    }

    @Test
    public void promptBillable() throws Exception {
        assertEquals(encounterPresenter.promptBillable("FOO").getClass(), Billable.class);
        assertEquals(encounterPresenter.promptBillable("FOO").toString(), "Select a foo...");
    }

    @Test
    public void getBillablesList() throws Exception {
        List<Billable> billables = new ArrayList<>();
        Billable fakeLab1 = new Billable();
        Billable fakeLab2 = new Billable();
        Billable fakeLab3 = new Billable();
        fakeLab1.setName("fake lab 1");
        fakeLab2.setName("fake lab 2");
        fakeLab3.setName("fake lab 3");
        billables.add(fakeLab1);
        billables.add(fakeLab2);
        billables.add(fakeLab3);

        when(BillableDao.getBillablesByCategory(Billable.TypeEnum.LAB)).thenReturn(billables);

        List<Billable> billablesList = encounterPresenter.getBillablesList("LAB");

        assertEquals(billablesList.toString(), "[Select a lab..., fake lab 1, fake lab 2, fake lab 3]");
    }
}
