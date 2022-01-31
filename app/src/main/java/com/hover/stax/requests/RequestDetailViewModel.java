package com.hover.stax.requests;

import static org.koin.java.KoinJavaComponent.get;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.hover.stax.channels.Channel;
import com.hover.stax.contacts.StaxContact;
import com.hover.stax.database.DatabaseRepo;

import java.util.List;

public class RequestDetailViewModel extends AndroidViewModel {

    private final MutableLiveData<Request> request;
    private final DatabaseRepo repo = get(DatabaseRepo.class);
    private final LiveData<Channel> channel;
    private LiveData<List<StaxContact>> recipients = new MutableLiveData<>();

    public RequestDetailViewModel(@NonNull Application application) {
        super(application);
        request = new MutableLiveData<>();
        channel = Transformations.switchMap(request, r -> repo.getLiveChannel(r.requester_institution_id));
        recipients = Transformations.switchMap(request, this::loadRecipients);
    }

    public LiveData<Request> getRequest() {
        if (request == null) {
            return new MutableLiveData<>();
        }
        return request;
    }

    public void setRequest(int id) {
        new Thread(() -> request.postValue(repo.getRequest(id))).start();
    }

    public LiveData<Channel> getChannel() {
        if (channel == null) {
            return new MutableLiveData<>();
        }
        return channel;
    }

    public LiveData<List<StaxContact>> loadRecipients(Request r) {
        return repo.getLiveContacts(r.requestee_ids.split(","));
    }

    public LiveData<List<StaxContact>> getRecipients() {
        if (recipients == null) {
            return new MutableLiveData<>();
        }
        return recipients;
    }

    void deleteRequest() {
        repo.delete(request.getValue());
    }
}
