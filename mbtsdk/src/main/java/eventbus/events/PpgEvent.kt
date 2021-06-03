package eventbus.events

import model.PpgFrame

data class PpgEvent(val data: PpgFrame) : IEvent
