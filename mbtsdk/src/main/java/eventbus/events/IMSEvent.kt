package eventbus.events

import model.Position3D

data class IMSEvent(val positions: ArrayList<Position3D>) : IEvent {}