// ログイン中の従業員IDをセッションストレージから取得する関数 (共通JSファイルに存在することを前提)
function getLoggedInEmployeeId() {
    const employeeId = sessionStorage.getItem('loggedInEmployeeId'); 
    if (!employeeId) {
        throw new Error("操作を行う従業員IDが見つかりません。ログインが必要です。");
    }
    return employeeId;
}

document.addEventListener('DOMContentLoaded', function () {
    const editForm = document.getElementById('attendance-edit-form');
    
    // URLからサマリーIDを取得
    const urlParams = new URLSearchParams(window.location.search);
    const summaryId = urlParams.get('summaryId');

    if (!summaryId) {
        alert("エラー: 修正対象の勤怠サマリーIDが指定されていません。");
        window.location.href = 'attendance_log.html';
        return;
    }

    // ------------------------------------------
    // 1. データ取得とフォームへの設定
    // ------------------------------------------
    // NOTE: DailyAttendanceSummaryServiceには単一サマリーを取得するAPIがないため、
    // ここではリスト取得APIを再利用するか、またはサーバー側で専用APIが必要です。
    // 今回は、仮のAPIエンドポイントを使います。
    fetchSummaryData(summaryId);

    // ------------------------------------------
    // 2. フォーム送信（更新API呼び出し）
    // ------------------------------------------
    editForm.addEventListener('submit', handleFormSubmit);

    /**
     * 勤怠サマリーデータをAPIから取得し、フォームに設定する
     * @param {string} id サマリーID
     */
    async function fetchSummaryData(id) {
        try {
            // 仮の単体取得API: /api/summaries/123 のようなAPIが必要
            // 実際には、Controller/Service/Repositoryの追加開発が必要です。
            const res = await fetch(`/api/summaries/${id}`); 
            
            if (!res.ok) {
                if (res.status === 404) {
                    throw new Error("指定された勤怠サマリーIDが見つかりません。");
                }
                throw new Error('勤怠データの取得に失敗しました。');
            }
            
            const summaryDto = await res.json();
            
            // DTOのデータを使ってフォームに設定
            document.getElementById('summaryId').value = summaryDto.id;
            document.getElementById('employeeId').value = summaryDto.employeeId;
            
            // 表示情報の更新
            document.getElementById('display-employee-name').textContent = summaryDto.employeeName;
            document.getElementById('display-work-date').textContent = summaryDto.workDate;
            
            // フォーム入力項目の設定
            // JSONの時刻文字列 (例: "09:00:00.000000") から HH:mm 部分を抽出
            document.getElementById('actualInTime').value = formatTimeForInput(summaryDto.actualInTime);
            document.getElementById('actualOutTime').value = formatTimeForInput(summaryDto.actualOutTime);
            document.getElementById('totalBreakMinutes').value = summaryDto.totalBreakMinutes;

        } catch (error) {
            alert(`データ取得エラー: ${error.message}`);
            console.error(error);
            window.location.href = 'attendance_log.html';
        }
    }
    
    /**
     * APIから取得した時刻文字列を <input type="time"> 用の HH:mm 形式に整形するヘルパー関数
     */
    function formatTimeForInput(timeString) {
        if (!timeString) return '';
        // "09:00:00.000000" -> "09:00"
        return timeString.substring(0, 5); 
    }


    /**
     * フォーム送信時の処理（更新APIの呼び出し）
     * @param {Event} event フォームイベント
     */
    async function handleFormSubmit(event) {
        event.preventDefault();

        const id = document.getElementById('summaryId').value;
        const employeeId = document.getElementById('employeeId').value;
        const inTime = document.getElementById('actualInTime').value;
        const outTime = document.getElementById('actualOutTime').value;
        const breakMinutes = parseInt(document.getElementById('totalBreakMinutes').value, 10);
        
        // 簡単な入力チェック
        if (breakMinutes < 0 || isNaN(breakMinutes)) {
            alert('休憩時間は0分以上の数値で入力してください。');
            return;
        }

        if (!confirm(`勤怠サマリーID: ${id} の出退勤情報を修正します。よろしいですか？`)) {
            return;
        }

        try {
            const updaterId = getLoggedInEmployeeId();
            
            // バックエンドに送信する更新DTOを構築
            const updateData = {
                id: parseInt(id, 10),
                employeeId: employeeId,
                // NOTE: Service側で時刻を再計算するため、ここでは必要な情報のみ送ります
                actualInTime: inTime + ":00", // "HH:mm:ss" 形式に修正
                actualOutTime: outTime + ":00",
                totalBreakMinutes: breakMinutes,
                // status: "PENDING" に戻すなどのロジックがService側で実行されます
            };

            // 仮の更新API: PUT /api/summaries/{id}
            const res = await fetch(`/api/summaries/${id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Operator-Id': updaterId
                },
                body: JSON.stringify(updateData)
            });

            if (res.ok) {
                alert(`✅ 勤怠サマリー ID:${id} が正常に修正・更新されました。`);
                // 更新完了後、勤怠ログ一覧画面に戻る
                window.location.href = 'attendance_log.html';
            } else {
                 const errorData = await res.json();
                 throw new Error(errorData.message || '勤怠修正処理中に不明なエラーが発生しました。');
            }

        } catch (error) {
            alert(`❌ 修正に失敗しました: ${error.message}`);
            console.error("Attendance update error:", error);
        }
    }
});