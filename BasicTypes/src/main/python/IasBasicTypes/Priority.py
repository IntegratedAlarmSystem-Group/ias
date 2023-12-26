'''
The priority of alarms as defined in org.eso.ias.types.Priority
'''

from enum import Enum

class Priority(Enum):
    LOW = 0
    MEDIUM = 1
    HIGH = 2
    CRITICAL = 3

    @staticmethod
    def value_of(name):
        '''
        Get and return the priority from the passed name.

        It delegates to Enum.__getitem__(cls, name)
        :param cls:  the class
        :param name: the name of the priority
        :return: the priority with the given name
        :raise KeyError if the name does not correspond to a priority
        '''
        return Priority.__getitem__(name)

    def __str__(self):
        return self.name

    def to_string(self):
        '''

        :param cls: the clss
        :return: the string representation of the priority
        '''
        return self.name

    @staticmethod
    def get_min_priority():
        '''
        :return: the minimum priority
        '''
        return Priority.LOW

    @staticmethod
    def get_max_priority():
        '''
        :return: the maximum priority
        '''
        return Priority.CRITICAL

    @staticmethod
    def get_default_priority():
        '''
        :return: the default priority
        '''
        return Priority.MEDIUM

    def get_higher_priority(self):
        '''
        :return: the priority higher than the priority of this
                 object or the maximum priority if the priority of this
                 object is the highest possible priority
        '''
        if self == Priority.CRITICAL:
            return self
        else:
            return list(Priority)[self.value+1]

    def get_lower_priority(self):
        '''
        :return: the priority lower than the priority of this
                 object or the minimum priority if the priority of this
                 object is the lowest possible priority
        '''
        if self == Priority.LOW:
            return self
        else:
            return list(Priority)[self.value-1]