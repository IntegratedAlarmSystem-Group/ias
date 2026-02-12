"""
Python equivalent of Identifier.scala
"""

import re
from typing import Optional, List, Union

from IasBasicTypes.IdentifierType import IdentifierType

class Identifier:
    # Constants
    separator = "@"
    coupleGroupPrefix = "("
    coupleGroupSuffix = ")"
    coupleSeparator = ":"
    templatedIdPrefix = "[!#"
    templateSuffix = "!]"
    fullRunningIdRegExp = r"\([^:^\(^\).]+:[^:^\(^\).]+\)(@\([^:^\(^\).]+:[^:^\(^\).]+\))*"
    templateRegExp = re.compile(r"\[!#\d+!\]")
    templatedIdRegExp = re.compile(r".+" + templateRegExp.pattern)

    def __init__(self, id: str, id_type: IdentifierType, parent_id: Optional['Identifier'] = None):
        if not id:
            raise ValueError("Invalid empty identifier")
        if Identifier.separator in id:
            raise ValueError(f"Invalid character {Identifier.separator} in identifier {id}")
        if Identifier.coupleSeparator in id:
            raise ValueError(f"Invalid character {Identifier.coupleSeparator} in identifier {id}")
        if Identifier.coupleGroupSuffix in id:
            raise ValueError(f"Invalid character {Identifier.coupleGroupSuffix} in identifier {id}")
        if Identifier.coupleGroupPrefix in id:
            raise ValueError(f"Invalid character {Identifier.coupleGroupPrefix} in identifier {id}")
        if not id_type:
            raise ValueError("Invalid identifier type")
        
        if parent_id is None:
            if not self.is_valid_parent_type(id_type, None):
                raise ValueError(f"Invalid parent None for {id_type}")
        else:
            if not self.is_valid_parent_type(id_type, parent_id.id_type):
                raise ValueError(f"Invalid parent {parent_id} for {id_type}")

        # The pattern of the template must appear at most once in the id
        if len(Identifier.templatedIdRegExp.findall(id)) > 1:
            raise ValueError(f"Template pattern mismatch in {id}")

        self.id: str = id
        self.id_type: IdentifierType = id_type
        self.parent_id = parent_id

        # self.from_template = Identifier.is_templated_identifier(id)
        # if self.from_template and not Identifier.can_be_templated(id_type):
        #     raise ValueError(f"{id_type} does not support template")

    @staticmethod
    def check_full_running_id_format(frid: str) -> bool:
        if not frid:
            raise ValueError("Invalid full running ID")
        return re.fullmatch(Identifier.fullRunningIdRegExp, frid) is not None

    # @staticmethod
    # def is_templated_identifier(id: str) -> bool:
    #     if not id:
    #         raise ValueError("Invalid identifier")
    #     return len(Identifier.templatedIdRegExp.findall(id)) == 1

    # @staticmethod
    # def get_template_instance(id: str) -> Optional[int]:
    #     if not id:
    #         raise ValueError("Invalid identifier")
    #     templates = Identifier.templateRegExp.findall(id)
    #     if len(templates) > 1:
    #         raise AssertionError(f"Templated mismatch identifier {id}")
    #     if templates:
    #         temp = templates[0]
    #         return int(temp.replace(Identifier.templatedIdPrefix, "").replace(Identifier.templateSuffix, ""))
    #     return None

    # @staticmethod
    # def get_base_id(id: str) -> str:
    #     if not id:
    #         raise ValueError("Invalid identifier")
    #     instance = Identifier.get_template_instance(id)
    #     if instance is not None:
    #         template_pattern = Identifier.build_template_pattern(instance)
    #         return id.replace(template_pattern, "")
    #     return id

    # @staticmethod
    # def can_be_templated(id_type: str) -> bool:
    #     if not id_type:
    #         raise ValueError("Invalid identifier type")
    #     return id_type in [IdentifierType.IASIO, IdentifierType.DASU, IdentifierType.ASCE]

    # @staticmethod
    # def build_template_pattern(instance: int) -> str:
    #     return f"{Identifier.templatedIdPrefix}{instance}{Identifier.templateSuffix}"

    # @staticmethod
    # def build_id_from_template(id: str, instance: Optional[int]) -> str:
    #     if not id or not id.strip():
    #         raise ValueError("Invalid identifier")
    #     if instance is not None:
    #         if Identifier.is_templated_identifier(id):
    #             raise ValueError(f"{id} is already generated from a template")
    #         return f"{id.strip()}{Identifier.build_template_pattern(instance)}"
    #     return id

    @classmethod
    def from_string(cls, full_running_id: str) -> 'Identifier':
        if not full_running_id:
            raise ValueError("Invalid full running ID")
        if not Identifier.check_full_running_id_format(full_running_id):
            raise ValueError(f"Invalid fullRunningId format {full_running_id}")

        identifiers_descr = full_running_id.split(cls.separator)
        parent = None
        for couple in identifiers_descr:
            print(f"Working on couple: {couple} with parent {parent}")
            cleaned_couple = couple[len(cls.coupleGroupPrefix):-len(cls.coupleGroupSuffix)]
            id_str, type_str = cleaned_couple.split(cls.coupleSeparator)
            parent = Identifier(id_str, IdentifierType.from_string(type_str), parent)
        return parent

    def is_valid_parent_type(self, the_type: IdentifierType, parent_type: Optional[IdentifierType]) -> bool:
        if the_type is None:
            raise ValueError("Invalid null identifier type")
    
        if the_type.parents == []:
            return parent_type is None
        else:
            return parent_type in the_type.parents

    def build_formatted_id(self, format_func) -> str:
        if self.parent_id:
            return self.parent_id.build_formatted_id(format_func) + Identifier.separator + format_func(self)
        return format_func(self)

    def build_full_running_id(self) -> str:
        return self.build_formatted_id(
            lambda ide: f"{Identifier.coupleGroupPrefix}{ide.id}{Identifier.coupleSeparator}{ide.id_type.name}{Identifier.coupleGroupSuffix}"
        )

    def build_running_id(self) -> str:
        """
        Build and return the running id i.e. something like "SupervisorWithKafka@DasuStrenght@AsceStrenght"
        """
        return self.build_formatted_id(lambda ide: ide.id)

    def __str__(self):
        return self.build_full_running_id()

    def get_id_of_type(self, id_type_to_search: IdentifierType) -> Optional[str]:
        """
        Get and return the id of the given type in the current identifier or in one of its parents.
        """
        if not id_type_to_search:
            raise ValueError("Cannot search for an undefined identifier type")
        if id_type_to_search == self.id_type:
            return self.id
        else:
            if self.parent_id is None:
                return None
            else:
                return self.parent_id.get_id_of_type(id_type_to_search)
    