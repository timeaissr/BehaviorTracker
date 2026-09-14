package com.github.timeaissr.behaviortracker.ui.add;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.github.timeaissr.behaviortracker.data.entity.Behavior;
import com.github.timeaissr.behaviortracker.data.repository.BehaviorRepository;

public class AddBehaviorViewModel extends AndroidViewModel {

    private final BehaviorRepository repository;
    private final MutableLiveData<Boolean> saveComplete = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deleteComplete = new MutableLiveData<>();
    private long editingBehaviorId = -1;

    public AddBehaviorViewModel(@NonNull Application application) {
        super(application);
        repository = new BehaviorRepository(application);
    }

    public LiveData<Boolean> getSaveComplete() {
        return saveComplete;
    }

    public LiveData<Boolean> getDeleteComplete() {
        return deleteComplete;
    }

    public void setEditingBehaviorId(long id) {
        this.editingBehaviorId = id;
    }

    public long getEditingBehaviorId() {
        return editingBehaviorId;
    }

    public boolean isEditing() {
        return editingBehaviorId > 0;
    }

    public LiveData<Behavior> getBehavior(long id) {
        return repository.getBehaviorById(id);
    }

    public void saveBehavior(Behavior behavior) {
        if (isEditing()) {
            repository.updateBehavior(editingBehaviorId, behavior, saveComplete::postValue);
        } else {
            repository.insertBehavior(behavior, id -> saveComplete.postValue(id > 0));
        }
    }

    public void deleteBehavior(long behaviorId) {
        repository.deleteBehavior(behaviorId, deleteComplete::postValue);
    }

    @Override
    protected void onCleared() {
        repository.shutdown();
    }
}
