from configs import get_configs, start_value, end_value, step, normalize_json, popx, sorting_orders, arg_mapp
import csv


class DataEntity:
    """
    Class to manage input data
    has id - the file name
    size - file size
    prediction - file prediction
    actual - file is buggy or not (actual truth)
    order_id - the order in which they come, neccessary in some cases
    order_cnt - helps with order , 0 - order by prediction, 1 - order by predicted order (buggy,size),
                                   2 - order by actual, 3 - order by reverse actual
                                       """
    order_cnt = 0

    def __init__(self, id="", size=0, prediction=0.0, actual=True, prediction_1=False, order_id=0, prediction_size=0):
        """
        Creates DataEntity Instance (ignore)
        :param id:
        :param size:
        :param prediction:
        :param actual:
        :param order_id:
        """
        self.id = id
        self.size = size
        self.prediction = prediction
        self.actual = actual
        self.order_id = order_id
        self.prediction_1 = prediction_1
        self.prediction_size = prediction_size

    def __str__(self):
        """

        :return: String information of the data
        """
        return f"{self.id} ---- {self.size} ---- {self.prediction} ---- {self.actual} ---- {self.order_id}"

    def __lt__(self, other):
        """ Helper function for sort (defines DataEntity A smaller then DataEntity B through rules defined in predicted
        order , ignore"""
        result = True
        order = sorting_orders[self.order_cnt]
        for equality_check, sign in order.items():
            value1 = getattr(self, equality_check)
            value2 = getattr(other, equality_check)
            if value1 < value2:
                result = True if sign else False
                break
            elif value1 > value2:
                result = False if sign else True
                break
        return result

    def __eq__(self, other):
        """
        Defines equality , ignore
        :param other:
        :return:
        """
        return self.id == other.id


class ProcessedDataEntity:
    """
    ProcessDataEntity Class - for output created
    filename - filename
    ifa - ifa
    poptStepData - a dictionary of popX for x in range(10-50) by default or the valus defined by you in configs)
    precision_0.5 - precison0.5
    recall - recall
    f1score - F1Score
    map - map
    auc = Area under curve
    g_measure - G measure
    mcc - Mathews corroltion coeficinet
    """
    args = None

    def __init__(self, filename="", total_nr_no=0, IFA=0, poptStepData={}, PoptXData={}, NPofBData={}, NpoptData={},precision_0_5=0.0, recall=0.0,
                 f1_score=0.0,
                 MAP=0.0, auc=0,
                 g_measure=0, mcc=0, pop=0, average_pop=0, average_pofb=0 ,average_npofb=0, average_npopt=0):
        """
        Creates instance, ignore
        :param filename:
        :param IFA:
        :param poptStepData:
        :param precision_0_5:
        :param recall:
        :param f1_score:
        :param MAP:
        :param auc:
        :param g_measure:
        :param mcc:
        """
        self.filename = filename
        self.total_nr_no = total_nr_no
        self.IFA = IFA
        for key, value in poptStepData.items():
            setattr(self, key, value)
        for key, value in PoptXData.items():
            setattr(self, key, value)
        for key, value in NPofBData.items():
            setattr(self, key, value)
        for key, value in NpoptData.items():
            setattr(self, key, value)
        self.precision_0_5 = precision_0_5
        self.recall = recall
        self.f1_score = f1_score
        self.MAP = MAP
        self.popStepData = poptStepData
        self.PoptXData = PoptXData
        self.NPofBData = NPofBData
        self.NpoptData = NpoptData
        self.auc = auc
        self.g_measure = g_measure
        self.mcc = mcc
        self.pop = pop
        self.average_pop = average_pop
        self.average_pofb = average_pofb
        self.average_npofb = average_npofb
        self.average_npopt = average_npopt

    def normalize(self):
        """Function that for each attribute in the original field does the normalization according to optimal/worst
        values defined in the type of attribute (be it IFA, popt and so on)"""
        normalized_proccessed_entity = ProcessedDataEntity(filename=self.filename, poptStepData=self.popStepData)
        for attr, values in normalize_json.items():
            optimal = values.get('optimal')
            if optimal == 'x':
                optimal = self.total_nr_no
            worst = values.get('worst')
            if worst == 'x':
                worst = self.total_nr_no
            normalized_value = 1 - (optimal - getattr(self, attr)) / (optimal - worst) if (optimal - worst) else 1
            setattr(normalized_proccessed_entity, attr, normalized_value)

        for key, value in self.popStepData.items():
            optimal = popx.get('optimal')
            if optimal == 'x':
                optimal = self.total_nr_no
            worst = popx.get('worst')
            if worst == 'x':
                worst = self.total_nr_no
            normalized_value = 1 - (optimal - getattr(self, key)) / (optimal - worst)
            setattr(normalized_proccessed_entity, key, normalized_value)

        for key, value in self.PoptXData.items():
            optimal = popx.get('optimal')
            if optimal == 'x':
                optimal = self.total_nr_no
            worst = popx.get('worst')
            if worst == 'x':
                worst = self.total_nr_no
            normalized_value = 1 - (optimal - getattr(self, key)) / (optimal - worst)
            setattr(normalized_proccessed_entity, key, normalized_value)

        for key, value in self.NpoptData.items():
            optimal = popx.get('optimal')
            if optimal == 'x':
                optimal = self.total_nr_no
            worst = popx.get('worst')
            if worst == 'x':
                worst = self.total_nr_no
            normalized_value = 1 - (optimal - getattr(self, key)) / (optimal - worst)
            setattr(normalized_proccessed_entity, key, normalized_value)

        for key, value in self.NPofBData.items():
            optimal = popx.get('optimal')
            if optimal == 'x':
                optimal = self.total_nr_no
            worst = popx.get('worst')
            if worst == 'x':
                worst = self.total_nr_no
            normalized_value = 1 - (optimal - getattr(self, key)) / (optimal - worst)
            setattr(normalized_proccessed_entity, key, normalized_value)

    def get_popX(self):
        poptX = ""
        for key, val in self.popStepData.items():
            poptX += f"{val};"
        return poptX

    def get_popTX(self):
        poptX = ""
        for key, val in self.PoptXData.items():
            poptX += f"{val};"
        return poptX

    def get_NPofBData(self):
        poptX = ""
        for key, val in self.NPofBData.items():
            poptX += f"{val};"
        return poptX

    def get_NpoptData(self):
        poptX = ""
        for key, val in self.NpoptData.items():
            poptX += f"{val};"
        return poptX

    @staticmethod
    def get_popX_header(normalized, n=False):
        poptX = ""
        s = start_value
        while s <= end_value:
            word = f"NPofB{s};" if n else f"PofB{s};"
            appendage = f"{word}" if not normalized else f"norm{word};"
            poptX += appendage
            s += step
        return poptX

    @staticmethod
    def get_popTX_header(normalized, n=False):
        poptX = ""
        s = start_value
        while s <= end_value:
            word = f"NPopt{s};" if n else f"Popt{s};"
            appendage = f"{word}" if not normalized else f"norm{word};"
            poptX += appendage
            s += step
        return poptX

    @staticmethod
    def get_NPofBData_header(normalized, n=False):
        poptX = ""
        s = start_value
        while s <= end_value:
            word = f"NPofb{s};" if n else f"Npofb{s};"
            appendage = f"{word}" if not normalized else f"norm{word};"
            poptX += appendage
            s += step
        return poptX

    @staticmethod
    def get_NpoptData_header(normalized, n=False):
        poptX = ""
        s = start_value
        while s <= end_value:
            word = f"NPopt{s};" if n else f"Popt{s};"
            appendage = f"{word}" if not normalized else f"norm{word};"
            poptX += appendage
            s += step
        return poptX

    def __str__(self):
        """
        :return: String information of processed , i use it when i write in files
        """
        args = self.args if self.args else arg_mapp

        output = f"{self.filename};"

        for arg in args:
            if arg == "poptX":
                output += self.get_popX()
            elif arg == "popTX":
                output += self.get_popTX()
            elif arg == "Npopt":
                output += self.get_NpoptData()
            elif arg == "NPofB":
                output += self.get_NPofBData()
            else:
                output += f"{getattr(self, arg_mapp[arg])};"

        return output

    @staticmethod
    def generate_header(normalized=False, n=False):
        """generates first row of new file, if normalized=True - adds norm as a name"""

        args = ProcessedDataEntity.args if ProcessedDataEntity.args else arg_mapp
        appendage = "norm" if normalized else ""
        start_appendage = "N" if n else ""
        output = "Filename;"

        for arg in args:
            if arg == "poptX":
                output += ProcessedDataEntity.get_popX_header(normalized, n)
            elif arg == "popTX":
                output += ProcessedDataEntity.get_popTX_header(normalized, n)
            elif arg == 'Npopt':
                output += ProcessedDataEntity.get_NpoptData_header(normalized, n)
            elif arg == "NPofB":
                output += ProcessedDataEntity.get_NPofBData_header(normalized, n)
            else:
                output += f"{start_appendage}{appendage}{arg};"
        return output


def get_index_value(predicted, actual):
    """
    Helper function to find mapping for predicted and actual (True negative, true positive, false negative, false positive)
    :param predicted:
    :param actual:
    :return:
    """
    if predicted and actual:
        index = "TP"
    elif predicted and not actual:
        index = "FP"
    elif not predicted and actual:
        index = "FN"
    elif not predicted and not actual:
        index = "TN"
    else:
        index = "NA"

    return index


def calculate_MAP(TRUE_FALSE_MAPPINGS):
    """
    Map calculator depending on data
    :param TRUE_FALSE_MAPPINGS:
    :return:
    """
    MAP = 0
    for TRUE_FALSE_MAPPING in TRUE_FALSE_MAPPINGS:
        tpc = TRUE_FALSE_MAPPING.get('TP', 0)
        fpc = TRUE_FALSE_MAPPING.get('FP', 0)
        if tpc + fpc:
            average = (tpc / (tpc + fpc))
            MAP += average
    return MAP / 11


def calculate_AUC(AUC_MAPPINGS):
    """
    Calculates all TPR and FPR for step 0.01 from 0 to 1, for each calc
    :param AUC_MAPPINGS:
    :return:
    """
    AUC = 0

    for index, AUC_MAPPING in enumerate(AUC_MAPPINGS[0:-1]):
        tpc = AUC_MAPPING.get('TP', 0)
        tnc = AUC_MAPPING.get('TN', 0)
        fpc = AUC_MAPPING.get('FP', 0)
        fnc = AUC_MAPPING.get('FN', 0)

        tpr = tpc / (tpc + fnc) if tpc + fnc else 0
        fpr = fpc / (fpc + tnc) if fpc + tnc else 0

        ftnc = AUC_MAPPINGS[index + 1].get('TN', 0)
        ffpc = AUC_MAPPINGS[index + 1].get('FP', 0)
        future_fpr = ffpc / (ffpc + ftnc) if ffpc + ftnc else 0
        AUC_DELTA = (fpr - future_fpr) * tpr

        AUC += AUC_DELTA
    return AUC


def calculate_POP(PopTotalData, PopOptimalData, PopWorseData):
    """ calculates POP, using partial AP/AO/AW, ignoring the un neccessary data"""
    AreaPopTotal, AreaPopOptimal, AreaPopWorse = 0, 0, 0
    parity_cnt = 0
    for key in PopTotalData.keys():
        if parity_cnt % 2 == 1:
            parity_cnt += 1
            continue
        AreaPopTotal += PopTotalData[key] * 0.1
        AreaPopOptimal += PopOptimalData[key] * 0.1
        AreaPopWorse += PopWorseData[key] * 0.1
        parity_cnt += 1
    if AreaPopOptimal == AreaPopWorse:
        return 1
    else:
        return 1 - ((AreaPopOptimal - AreaPopTotal) / (AreaPopOptimal - AreaPopWorse))


def get_steps(start, end, step, base_dict):
    """out of the total genrated pop data (from 10 to 100) takes (from start to end)"""
    poptStepData = {}

    for key, val in base_dict.items():

        length = len(str(start))
        last_digits = key[-length:]
        if last_digits.isnumeric() and int(last_digits) == start:
            poptStepData[key] = val

            start += step
            if start > end:
                break

    return poptStepData


def create_file(processed_csv_file_data, normalized=False, n=False):
    """
    Creates file with data, uses normalize or no just for name
    :param n: testing
    :param args: columns to create
    :param processed_csv_file_data:
    :param normalized:
    :return:
    """
    output_file_name = "EAM_NEAM_output.csv"
    if n:
        output_file_name = "N" + output_file_name  # file name
    if normalized:
        output_file_name = "norm_" + output_file_name  # normalized file name addition
    f = open(output_file_name, "w")
    header = ProcessedDataEntity.generate_header(normalized, n)
    writer = csv.writer(f)
    writer.writerow(header.split(";"))
    writer.writerows([x.__str__().split(";") for x in processed_csv_file_data])
    f.close()
