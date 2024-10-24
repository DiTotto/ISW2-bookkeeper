import math
import time
import sys
from os import listdir
from os.path import isfile, join

from configs import files_path, include_types, start_value, end_value, step, exclude_strings
from models import DataEntity, ProcessedDataEntity, get_index_value, calculate_MAP, create_file, calculate_AUC, \
    get_steps, calculate_POP



ProcessedDataEntity.args = sys.argv[1:]
processed_csv_file_data = []
processed_csv_file_data_normalized = []
mypath = files_path
onlyfiles = [f for f in listdir(mypath) if isfile(join(mypath, f))]
t = time.time()
data_rows = []
delimiter = ","
for file_num, filename in enumerate(onlyfiles):
    TRUE_FALSE_MAPPINGS = [{} for j in range(0, 11)]
    AUC_MAPPINGS = [{} for j in range(0, 101)]
    extension = filename.split(".")[1]
    if extension not in include_types:
        continue
    exclude_check = False
    for excluder in exclude_strings:
        if excluder in filename:
            exclude_check = True
            break
    if exclude_check:
        continue

    file = open(mypath + filename)
    total_line_of_code = 0  # total lines of code
    total_nr_no = 0  # total line of false rows (used for normalization of IFA)
    """
    File reading and initial transformation
    """

    for row_index, row in enumerate(file):
        if row_index == 0:
            continue
        lst = row.split(delimiter)  # delimiter
        id, size, prediction, actual = lst[0], float(lst[1]) if lst[1] else 0, float(lst[2]), True if lst[
                                                                                                          3].strip().upper() == 'YES' else False
        if size != 0:

            data_rows.append(DataEntity(id=id, size=size, prediction=prediction, actual=actual, prediction_1=not actual,
                                        order_id=row_index , prediction_size=prediction / size))
            total_line_of_code += data_rows[-1].size
            total_nr_no += 1 if not actual else 0

        for i in range(0, 11):
            predicted = True if prediction >= i / 10 else False
            index = get_index_value(predicted, actual)
            if index in TRUE_FALSE_MAPPINGS[i]:
                TRUE_FALSE_MAPPINGS[i][index] += 1
            else:
                TRUE_FALSE_MAPPINGS[i][index] = 1

        for i in range(0, 101):
            predicted = True if prediction >= i / 100 else False
            index = get_index_value(predicted, actual)
            if index in AUC_MAPPINGS[i]:
                AUC_MAPPINGS[i][index] += 1
            else:
                AUC_MAPPINGS[i][index] = 1

    print("Processing file # ", file_num + 1, filename, total_line_of_code, len(data_rows))

    DataEntity.order_cnt = 0
    data_rows = sorted(data_rows)
    """
    Calculate IFA
    """
    IFA_CNT = 0
    for data in data_rows:
        if not data.actual:
            IFA_CNT += 1
        else:
            break

    """
    Iterate and get POPX, incrementing start value with step till it becomes = end_value
    This function will also generate Pop general, Worst , Optimal by sorting according to the defined fields
    """


    def getPOPTWithSteps(start_value, end_value, step):

        def generate(ind):
            """
            Method that depending on the indes, generates either the nominators for each step(goes by provided step)
            for
            POP*OPTIMAL*WORSE
            :param ind:
            :return:
            """
            index_pos, partial_sum, entity_cnt = 0, 0, 0
            active_rows = data_rows if not ind else predicted_order_data_rows
            data_dict = poptDictDataTotal if ind == 0 else optimalDictData if ind == 1 else worstDictData

            for index_value, value in enumerate(check_total_values):
                while index_pos < len(active_rows):
                    partial_sum += active_rows[index_pos].size
                    if partial_sum >= value:
                        key = list(poptDictDataTotal.keys())[index_value]
                        data_dict[key] = entity_cnt
                        partial_sum -= active_rows[index_pos].size
                        break

                    entity_cnt += 1 if active_rows[index_pos].actual else 0
                    index_pos += 1

        real_start = 10
        real_end = 100
        check_total_values = []
        poptDictDataTotal, optimalDictData, worstDictData, popXDictData = {}, {}, {}, {}
        while real_start <= real_end:
            check_total_values.append(real_start * total_line_of_code / 100)
            poptDictDataTotal[f"POPT{real_start}"] = 0
            optimalDictData[f"POPT{real_start}"] = 0
            worstDictData[f"POPT{real_start}"] = 0
            popXDictData[f"POPT{real_start}"] = 0
            real_start += 5

        generate(0)  # Prediction

        DataEntity.order_cnt = 2
        predicted_order_data_rows = sorted(data_rows)
        generate(1)  # Optimal

        DataEntity.order_cnt = 3
        predicted_order_data_rows = sorted(data_rows)
        generate(2)  # Worse
        worstDictData['POPT100'] = poptDictDataTotal.get(list(poptDictDataTotal.keys())[-1])  # MOMENTARY BUG FIX
        DataEntity.order_cnt = 1
        index_pos, partial_sum, entity_cnt = 0, 0, 0
        predicted_order_data_rows = sorted(data_rows)
        # Calculates the denominator and does the calculations nom/denom
        for index_value, value in enumerate(check_total_values):
            while index_pos < len(predicted_order_data_rows):
                partial_sum += predicted_order_data_rows[index_pos].size
                if partial_sum >= value:
                    key = list(poptDictDataTotal.keys())[index_value]
                    end_all = list(poptDictDataTotal.keys())[-1]
                    divider = poptDictDataTotal[end_all]
                    if divider:
                        poptDictDataTotal[key] /= divider  # Normal division now
                        worstDictData[key] /= divider
                        optimalDictData[key] /= divider
                        # print(key, optimalDictData[key], poptDictDataTotal[key], worstDictData[key])
                        popXDictData[key] = 1 - ((optimalDictData[key] - poptDictDataTotal[key]) / (
                                optimalDictData[key] - worstDictData[key])) if optimalDictData[key] - worstDictData[
                            key] != 0 else 1
                        if popXDictData[key] < 0:
                            popXDictData[key] = 0
                    else:
                        poptDictDataTotal[key] = 0
                        worstDictData[key] = 0
                        optimalDictData[key] = 0
                        popXDictData[key] = 1

                    partial_sum -= predicted_order_data_rows[index_pos].size
                    break

                entity_cnt += 1 if predicted_order_data_rows[index_pos].actual else 0
                index_pos += 1

        return get_steps(start_value, end_value, step,
                         poptDictDataTotal), poptDictDataTotal, optimalDictData, worstDictData, popXDictData


    poptStepData, PopTotalData, PopOptimalData, PopWorseData, PoptXData = getPOPTWithSteps(start_value, end_value, step)

    # Confusion Matrix: 5 stands for 0.5 prediction , count of true positive, true negative, false positive, false negative
    tpc = TRUE_FALSE_MAPPINGS[5].get('TP', 0)
    fpc = TRUE_FALSE_MAPPINGS[5].get('FP', 0)
    fnc = TRUE_FALSE_MAPPINGS[5].get('FN', 0)
    tnc = TRUE_FALSE_MAPPINGS[5].get('TN', 0)

    # calculates precision_0.5 ,recall, f1_score, g_measure,mcc
    precision_0_5 = tpc / (tpc + fpc) if (tpc + fpc) else 0
    recall = tpc / (tpc + fnc) if (tpc + fnc) else 0
    f1_score = 2 * tpc / (2 * tpc + fpc + fnc) if (tpc + fpc + fnc) else 0
    pf = fpc / (fpc + tnc) if (fpc + tnc) else 0

    divider = math.sqrt((tpc + fpc) * (tpc + fnc) * (tnc + fpc) * (tnc + fnc))

    g_measure = 2 * recall * (1 - pf) / (recall + (1 - pf)) if (recall + (1 - pf)) else 0
    mcc = (tpc * tnc - fpc * fnc) / divider if divider else 0

    # calculates map,auc,pop
    MAP = calculate_MAP(TRUE_FALSE_MAPPINGS)
    auc = calculate_AUC(AUC_MAPPINGS)
    pop = calculate_POP(PopTotalData, PopOptimalData, PopWorseData)
    average_pop = sum(PoptXData.values()) / len(PoptXData.keys())
    average_pofb = sum(poptStepData.values()) / len(poptStepData.keys())

    DataEntity.order_cnt = 4
    data_rows = sorted(data_rows)
    poptStepData, NPofBData, PopOptimalData, PopWorseData, NpoptData = getPOPTWithSteps(start_value, end_value, step)
    average_npopt = sum(NpoptData.values()) / len(NpoptData.keys())
    average_npofb = sum(NPofBData.values()) / len(NPofBData.keys())

    processed_csv_file_data.append(
        ProcessedDataEntity(filename=filename, total_nr_no=total_nr_no, IFA=IFA_CNT,
                            precision_0_5=precision_0_5, recall=recall,f1_score=f1_score,
                            MAP=MAP, auc=auc, g_measure=g_measure, mcc=mcc, pop=pop,
                            poptStepData=poptStepData, PoptXData=PoptXData,NPofBData=NPofBData,  NpoptData=NpoptData,
                            average_pop=average_pop, average_pofb=average_pofb, average_npofb=average_npofb, average_npopt=average_npopt))
    processed_csv_file_data_normalized.append(processed_csv_file_data[-1].normalize())
    data_rows = []

"""
Small time calculation of file creation
"""
print(time.time() - t)

"""
Creates the non normalized file
"""
create_file(processed_csv_file_data)

"""
Creates the normalized file
"""
create_file(processed_csv_file_data_normalized, normalized=True)
