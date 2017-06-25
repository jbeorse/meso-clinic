package org.watsi.uhp.offline;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.watsi.uhp.IdentificationEventFactory;
import org.watsi.uhp.R;
import org.watsi.uhp.activities.ClinicActivity;
import org.watsi.uhp.database.BillableDao;
import org.watsi.uhp.database.IdentificationEventDao;
import org.watsi.uhp.database.MemberDao;
import org.watsi.uhp.models.AbstractModel;
import org.watsi.uhp.models.Billable;
import org.watsi.uhp.models.IdentificationEvent;
import org.watsi.uhp.models.Member;

import java.sql.SQLException;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.withDecorView;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.watsi.uhp.CustomMatchers.withAdaptedData;
import static org.watsi.uhp.CustomMatchers.withBillableName;
import static org.watsi.uhp.CustomMatchers.withEncounterItemName;

@RunWith(AndroidJUnit4.class)
public class EncounterFlowFeature extends BaseTest {

    private Member member;
    private Billable billableDrug;
    private Billable billableLab;
    private Billable billableSupply;
    private IdentificationEvent idEvent;

    @Rule
    public ActivityTestRule<ClinicActivity> clinicActivityRule =
            new ActivityTestRule<>(ClinicActivity.class, false, false);

    @Before
    public void setUpTest() throws SQLException, AbstractModel.ValidationException {
        member = MemberDao.all().get(0);
        billableDrug = BillableDao.getBillablesByCategory(Billable.TypeEnum.DRUG).get(0);
        billableLab = BillableDao.getBillablesByCategory(Billable.TypeEnum.LAB).get(0);
        billableSupply = BillableDao.getBillablesByCategory(Billable.TypeEnum.SUPPLY).get(0);

        idEvent = new IdentificationEventFactory(
                member,
                30
        );
        IdentificationEventDao.create(idEvent);

        clinicActivityRule.launchActivity(null);
    }

    public void addBillable(Billable billable) {
        onView(withId(R.id.category_spinner)).perform(click());
        onData(allOf(is(instanceOf(String.class)),
                is(billable.getType().toString())))
                .perform(click());

        if (billable.getType() == Billable.TypeEnum.DRUG) {
            onView(withId(R.id.drug_search)).perform(typeText(billable.getName()));
            onView(withText(billable.getName()))
                    .inRoot(withDecorView(not(clinicActivityRule.getActivity().getWindow()
                            .getDecorView())))
                    .perform(click());
        } else {
            onData(allOf(is(instanceOf(Billable.class)),
                    withBillableName(billable.getName())))
                    .perform(click());
        }
    }

    public void removeBillable(Billable billable) {
        onData(withEncounterItemName(billable.getName()))
                .inAdapterView(withId(R.id.line_items_list))
                .onChildView(withId(R.id.remove_line_item_btn))
                .perform(click());
    }

    public void assertEncounterItemInList(String encounterItemName) {
        onData(withEncounterItemName(encounterItemName))
                .inAdapterView(withId(R.id.line_items_list))
                .check(matches(isDisplayed()));
    }

    public void assertEncounterItemNotInList(String encounterItemName) {
        onView(withId(R.id.line_items_list))
                .check(matches(not(withAdaptedData(withEncounterItemName(encounterItemName)))));
    }

    public void assertDisplaysToast(int messageId) {
        onView(withText(messageId))
                .inRoot(withDecorView(not(clinicActivityRule.getActivity().getWindow()
                        .getDecorView())))
                .check(matches(isDisplayed()));
    }

    @Test
    public void createEncounter_outpatientEncounterFlow() {
        String defaultBillable1 = "Consultation";
        String defaultBillable2 = "Medical Form";
        String newBillableName = "New Billable";
        String newBillablePrice = "1000";

        // the user can see checked-in members
        onView(withText(member.getFullName())).check(matches(isDisplayed()));

        // when the user clicks on a checked-in member, their details appear
        onView(withText(member.getFullName())).perform(click());
        onView(withText(R.string.detail_fragment_label)).check(matches(isDisplayed()));

        // when the user proceeds to enter encounter information, a list of encounter items
        // appears with the default billables
        onView(withText(R.string.detail_create_encounter)).perform(click());
        onView(withText(R.string.encounter_fragment_label)).check(matches(isDisplayed()));
        assertEncounterItemInList(defaultBillable1);
        assertEncounterItemInList(defaultBillable2);

        // TODO: defaults do not appear for delivery

        // the user can billables to the list of encounters items
        addBillable(billableLab);
        assertEncounterItemInList(billableLab.getName());

        addBillable(billableSupply);
        assertEncounterItemInList(billableSupply.getName());

        addBillable(billableDrug);
        assertEncounterItemInList(billableDrug.getName());

        // if the user selects the same billable twice, an error message appears
        addBillable(billableLab);
        assertDisplaysToast(R.string.already_in_list_items);

        // the user can remove billables
        removeBillable(billableSupply);
        assertEncounterItemNotInList(billableSupply.getName());

        removeBillable(billableDrug);
        assertEncounterItemNotInList(billableDrug.getName());

        // the user can add a billable again after removing it
        addBillable(billableSupply);
        assertEncounterItemInList(billableSupply.getName());

        // the user can add a new billable with a custom name and amount
        onView(withId(R.id.add_billable_prompt)).perform(click());
        onView(withId(R.id.name_field)).perform(typeText(newBillableName));
        onView(withId(R.id.price_field)).perform(typeText(newBillablePrice));
        onView(withId(R.id.save_button)).perform(click());
        onData(withEncounterItemName(newBillableName))
                .inAdapterView(withId(R.id.line_items_list))
                .check(matches(isDisplayed()));

        // TODO: the user can change the quantity of drugs to a positive number

        // TODO: the user cannot change the quantity of non-drugs

        // TODO: the user can change the date of the encounter for backfilling

        // the user can continue to take a form of the encounter
        onView(withText(R.string.continue_encounter_button)).perform(click());
        onView(withText(R.string.encounter_form_fragment_label)).check(matches(isDisplayed()));
        onView(withText(R.string.encounter_form_finish_btn)).perform(click());

        // the user can review all entered encounter line items in the receipt fragment and submit
        onView(withText(R.string.receipt_fragment_label)).check(matches(isDisplayed()));
        onView(withText(defaultBillable1)).check(matches(isDisplayed()));
        onView(withText(defaultBillable2)).check(matches(isDisplayed()));
        onView(withText(billableLab.getName())).check(matches(isDisplayed()));
        onView(withText(billableSupply.getName())).check(matches(isDisplayed()));
        onView(withText(newBillableName)).check(matches(isDisplayed()));
        onView(withText(R.string.save_encounter_button)).perform(click());

        // no checked-in members
        onView(withText(R.string.current_patients_empty_text)).check(matches(isDisplayed()));
    }
}