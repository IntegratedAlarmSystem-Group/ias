from IasBasicTypes.IdentifierType import IdentifierType

class TestIdentifierType:

    def test_parents(self):
        assert IdentifierType.SUPERVISOR.parents == []
        assert IdentifierType.DASU.parents == [IdentifierType.SUPERVISOR]
        assert IdentifierType.ASCE.parents == [IdentifierType.DASU]
        assert IdentifierType.CORETOOL.parents == []
        assert IdentifierType.CLIENT.parents == []
        assert IdentifierType.IASIO.parents == [IdentifierType.CONVERTER, IdentifierType.ASCE, IdentifierType.CORETOOL]

    def test_from_string(self):
        assert IdentifierType.from_string("IASIO") == IdentifierType.IASIO
        assert IdentifierType.from_string("DASU") == IdentifierType.DASU
        try:
            IdentifierType.from_string("")
            assert False, "Expected ValueError for empty string"
        except ValueError:
            pass