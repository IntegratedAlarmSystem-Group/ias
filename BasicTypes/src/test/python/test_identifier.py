from IasBasicTypes.IdentifierType import IdentifierType
from IasBasicTypes.Identifier import Identifier

class TestIdentifier:
    
    def test_id_from_string(self):
        frid = "(SupervisorWithKafka:SUPERVISOR)@(DasuStrenght:DASU)@(AsceStrenght:ASCE)"
        id = Identifier.from_string(frid)
        print("Identifier", id.build_full_running_id())
        assert frid == id.build_full_running_id()

    def test_identifier(self):
        supervisor_id = Identifier("SupervisorID", IdentifierType.SUPERVISOR, None)
        dasu_id = Identifier("DasuID", IdentifierType.DASU, supervisor_id)
        asce_id = Identifier("AsceID", IdentifierType.ASCE, dasu_id)
        iasio_id = Identifier("IasioID", IdentifierType.IASIO, asce_id)

        frid = "(SupervisorID:SUPERVISOR)@(DasuID:DASU)@(AsceID:ASCE)@(IasioID:IASIO)"
        assert frid == iasio_id.build_full_running_id()

    def test_id_of_type(self):
        frid = "(SupervisorWithKafka:SUPERVISOR)@(DasuStrenght:DASU)@(AsceStrenght:ASCE)"
        id = Identifier.from_string(frid)
        assert id.get_id_of_type(IdentifierType.DASU) == "DasuStrenght"
        assert id.get_id_of_type(IdentifierType.SUPERVISOR) == "SupervisorWithKafka"
        assert id.get_id_of_type(IdentifierType.ASCE) == "AsceStrenght"
        assert id.get_id_of_type(IdentifierType.IASIO) is None

    def test_running_id(self):
        frid = "(SupervisorWithKafka:SUPERVISOR)@(DasuStrenght:DASU)@(AsceStrenght:ASCE)"
        id = Identifier.from_string(frid)
        assert id.build_running_id() == "SupervisorWithKafka@DasuStrenght@AsceStrenght"
