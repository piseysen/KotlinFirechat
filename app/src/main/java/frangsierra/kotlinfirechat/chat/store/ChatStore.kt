package frangsierra.kotlinfirechat.chat.store

import dagger.Binds
import dagger.Module
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import frangsierra.kotlinfirechat.chat.controller.ChatController
import frangsierra.kotlinfirechat.chat.controller.ChatControllerImpl
import frangsierra.kotlinfirechat.core.dagger.AppScope
import frangsierra.kotlinfirechat.profile.store.ProfileStore
import frangsierra.kotlinfirechat.session.store.SignOutAction
import io.reactivex.disposables.CompositeDisposable
import mini.Reducer
import mini.Store
import mini.taskRunning
import javax.inject.Inject

@AppScope
class ChatStore @Inject constructor(val controller: ChatController, val profileStore: ProfileStore) : Store<ChatState>() {

    @Reducer
    fun loadMessages(action: StartListeningChatMessagesAction): ChatState {
        controller.startListeningMessages()
        return state
    }

    @Reducer
    fun messagesReceived(action: MessagesLoadedAction): ChatState {
        return state.copy(messages = state.messages.plus(action.messages.map { it.uid to it }.toMap()))
    }

    @Reducer
    fun sendMessage(action: SendMessageAction): ChatState {
        controller.sendMessage(action.message, action.attachedImageUri, profileStore.state.publicProfile!!)
        return state.copy(sendMessageTask = taskRunning())
    }

    @Reducer
    fun messageSent(action: SendMessageCompleteAction): ChatState {
        return state.copy(sendMessageTask = action.task,
                messages = if (action.task.isSuccessful()) state.messages
                        .plus(action.message!!.uid to action.message) else state.messages)
    }

    @Reducer
    fun stopListeningMessages(action: StopListeningChatMessagesAction): ChatState {
        state.disposables.dispose()
        return state.copy(disposables = CompositeDisposable())
    }

    @Reducer
    fun signOut(action: SignOutAction): ChatState {
        return initialState()
    }
}

@Module
abstract class ChatModule {
    @Binds
    @AppScope
    @IntoMap
    @ClassKey(ChatStore::class)
    abstract fun provideChatStore(store: ChatStore): Store<*>

    @Binds
    @AppScope
    abstract fun bindChatController(impl: ChatControllerImpl): ChatController
}
