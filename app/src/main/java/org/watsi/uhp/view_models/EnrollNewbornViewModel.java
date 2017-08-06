package org.watsi.uhp.view_models;

import org.watsi.uhp.fragments.FormFragment;
import org.watsi.uhp.managers.ExceptionManager;
import org.watsi.uhp.models.AbstractModel;
import org.watsi.uhp.models.Member;

public class EnrollNewbornViewModel extends MemberViewModel {

    public EnrollNewbornViewModel(FormFragment<Member> formFragment, Member member) {
        super(formFragment, member);
    }

    public void updateSaveButton() {
        setSaveEnabled(validateFullName() && validateCardId());
    }

    public void onClickSave() {
        if (validateFullName() && validateGender() && validateCardId()) {
            getFormFragment().nextStep();
        }
    }
}
