document.addEventListener('DOMContentLoaded', () => {
    let selectedUserId = null;
    let selectedUserName = '';
    let selectedUserCode = '';

    const buttons = {
        '出勤': document.querySelector('.is-in'),
        '退勤': document.querySelector('.is-out'),
        '休憩開始': document.querySelector('.is-break-start'),
        '休憩終了': document.querySelector('.is-break-end')
    };

    // リアルタイム時計更新
    function updateTime() {
        const now = new Date();
        document.getElementById('current-time').textContent = now.toLocaleTimeString('ja-JP', { hour: '2-digit', minute: '2-digit' });
        document.getElementById('current-date').textContent = now.toLocaleDateString('ja-JP', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'short' });
    }
    setInterval(updateTime, 1000);
    updateTime();

    // 従業員リストの生成 (APIから取得)
    async function fetchAndRenderEmployees() {
        try {
            const response = await fetch('/api/employees');
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const employees = await response.json();
            
            const listElement = document.getElementById('employee-list');
            listElement.innerHTML = ''; // Clear existing list
            employees.forEach(emp => {
                const item = document.createElement('div');
                item.className = 'employee-item';
                item.textContent = emp.name;
                item.setAttribute('data-id', emp.id);
                item.setAttribute('data-name', emp.name);
                item.setAttribute('data-code', emp.employeeCode);
                item.onclick = () => selectEmployee(emp.id, emp.name, emp.employeeCode, item);
                listElement.appendChild(item);
            });
        } catch (error) {
            console.error('Failed to fetch employees:', error);
            const listElement = document.getElementById('employee-list');
            listElement.innerHTML = '<p style="color: red;">従業員リストの読み込みに失敗しました。</p>';
        }
    }

    // ボタンの状態を更新する
    async function updateButtonStates(employeeId) {
        // まずすべてのボタンを一旦有効化
        Object.values(buttons).forEach(btn => btn.disabled = false);

        if (!employeeId) {
            // 従業員が選択されていない場合はすべて非活性
            Object.values(buttons).forEach(btn => btn.disabled = true);
            return;
        }

        try {
            const response = await fetch(`/api/attendance/latest/${employeeId}`);
            if (response.ok) {
                const latestAttendance = await response.json();
                const lastStampType = latestAttendance.stampType;

                if (lastStampType === '出勤') {
                    buttons['出勤'].disabled = true;
                } else if (lastStampType === '退勤') {
                    // 1日の終わりなので、出勤以外はすべて非活性
                    buttons['退勤'].disabled = true;
                    buttons['休憩開始'].disabled = true;
                    buttons['休憩終了'].disabled = true;
                } else if (lastStampType === '休憩開始') {
                    buttons['出勤'].disabled = true;
                    buttons['休憩開始'].disabled = true;
                } else if (lastStampType === '休憩終了') {
                    buttons['出勤'].disabled = true;
                    buttons['休憩終了'].disabled = true;
                }
            } else if (response.status === 404) {
                // まだ打刻がない従業員
                buttons['退勤'].disabled = true;
                buttons['休憩開始'].disabled = true;
                buttons['休憩終了'].disabled = true;
            }
        } catch (error) {
            console.error('Error fetching latest attendance:', error);
            // エラー時は念のためすべて無効化
            Object.values(buttons).forEach(btn => btn.disabled = true);
        }
    }
    
    // 従業員選択時の処理
    window.selectEmployee = function(id, name, code, element) {
        selectedUserId = id;
        selectedUserName = name;
        selectedUserCode = code;
        document.getElementById('selected-name').textContent = name;
        
        // 選択状態のハイライト
        document.querySelectorAll('.employee-item').forEach(item => {
            item.classList.remove('is-selected');
        });
        element.classList.add('is-selected');

        document.getElementById('message-area').textContent = `${name}さんを選択しました。打刻ボタンを押してください。`;
        document.getElementById('message-area').className = 'message-box is-info';

        document.querySelector('.selected-user-info .prompt').style.display = 'none';
        updateButtonStates(id); // ボタンの状態を更新
    }

    // 打刻処理
    window.stamp = async function(type) {
        const messageArea = document.getElementById('message-area');
        
        if (selectedUserId === null) {
            messageArea.textContent = '⚠️ まずリストから自分の名前を選択してください！';
            messageArea.className = 'message-box is-error';
            return;
        }

        const attendanceData = {
            employeeId: selectedUserId,
            employeeCode: selectedUserCode,
            name: selectedUserName,
            attendanceType: type
        };

        try {
            const response = await fetch('/api/attendance/stamp', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(attendanceData),
            });

            const responseText = await response.text();

            if (response.ok) {
                messageArea.textContent = `✅ ${responseText}`;
                messageArea.className = 'message-box is-success';
                updateButtonStates(selectedUserId); // 打刻成功後にボタン状態を更新
            } else {
                throw new Error(responseText || 'サーバーでエラーが発生しました。');
            }

        } catch (error) {
            console.error('Stamp error:', error);
            messageArea.textContent = `❌ 打刻に失敗しました: ${error.message}`;
            messageArea.className = 'message-box is-error';
        }
    }

    fetchAndRenderEmployees();
    updateButtonStates(null); // 初期状態では出勤以外は非活性
});
