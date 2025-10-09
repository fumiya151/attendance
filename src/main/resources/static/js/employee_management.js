// employee_management.js

/**
 * セッションストレージからログイン中の従業員IDを取得する。
 * @returns {string} ログイン中の従業員ID
 * @throws {Error} 従業員IDが見つからない場合
 */
function getLoggedInEmployeeId() {
    const employeeId = sessionStorage.getItem('loggedInEmployeeId'); 
    if (!employeeId) {
        throw new Error("操作を行う従業員IDが見つかりません。ログインが必要です。");
    }
    return employeeId;
}

// 従業員一覧取得＆テーブル描画
async function fetchAndRenderEmployees() {
    try {
        // 全アクティブ従業員の限定情報を取得
        const res = await fetch('/api/employees');
        if (!res.ok) throw new Error('取得失敗');
        const employees = await res.json();
        
        // 念のためクライアント側でもアクティブな従業員のみをフィルタリング
        const activeEmployees = employees.filter(emp => emp.active === true);
        
        renderEmployeeRows(activeEmployees);
    } catch (err) {
        alert('従業員一覧の取得に失敗しました');
    }
}

// 検索・表示用関数
function renderEmployeeRows(list) {
    const tbody = document.querySelector('#employees .data-table tbody');
    
    // 既存行をシンプルにクリア
    tbody.innerHTML = '';
    
    list.forEach(emp => {
        const tr = document.createElement('tr');
        // IDの重複を避けるためクラスを使用
        tr.classList.add('employee-row');
        tr.dataset.id = emp.employeeId; 

        tr.innerHTML = `
            <td>${emp.employeeId || '---'}</td>
            <td>${emp.name || '---'}</td>
            <td>${emp.department || '---'}</td>
            <td>${emp.email || '---'}</td>
            <td>
                <button class="small-btn edit-btn"><i class="fas fa-pen"></i> 編集</button>
                <button class="small-btn delete-btn"><i class="fas fa-trash-alt"></i> 削除</button>
            </td>
        `;
        
        // 編集ボタンのイベントリスナー
        tr.querySelector('.edit-btn').addEventListener('click', () => {
            window.location.href = `edit_employee.html?employeeId=${emp.employeeId}`;
        });
        
        // 削除ボタン (論理削除/退職処理) のイベントリスナー
        tr.querySelector('.delete-btn').addEventListener('click', async (event) => {
            const deleteBtn = event.target.closest('.delete-btn');
            if (!deleteBtn) return; 

            const tr = deleteBtn.closest('tr');
            const employeeId = tr.dataset.id;
            
            if (confirm(`従業員コード: ${employeeId} の従業員を本当に削除しますか？`)) {
                try {
                    const operatorId = getLoggedInEmployeeId();
                    
                    const res = await fetch(`/api/employees/${employeeId}`, {
                        method: 'DELETE', 
                        headers: {
                            'X-Operator-Id': operatorId
                        }
                    });
                    
                    if (res.ok) {
                        alert('従業員の退職処理（論理削除）が完了しました。');
                        // リストを再描画して更新
                        fetchAndRenderEmployees(); 
                    } else {
                        const errorData = await res.json();
                        throw new Error(errorData.message || '退職処理に失敗しました。');
                    }
                } catch (err) {
                    alert(`処理エラー: ${err.message}`);
                }
            }
        });
        
        tbody.appendChild(tr);
    });
}

document.addEventListener('DOMContentLoaded', () => {
    // 初期表示
    fetchAndRenderEmployees();

    const periodSelect = document.getElementById('aggregation-period-select');

    // 期間選択リストの動的生成 (HTMLに要素が存在しない場合、このブロックはスキップされる)
    if (periodSelect) {
        periodSelect.innerHTML = ''; 
        
        const today = new Date();
        const currentYear = today.getFullYear();
        const currentMonth = today.getMonth();

        // 過去3ヶ月分のデータを生成
        for (let i = 0; i < 3; i++) {
            const targetDate = new Date(currentYear, currentMonth - i, 1);
            
            const year = targetDate.getFullYear();
            const month = targetDate.getMonth() + 1; 

            const monthString = `${year}年${month}月度`;
            
            const option = document.createElement('option');
            const valueString = `${year}-${String(month).padStart(2, '0')}`; 
            
            option.value = valueString;
            option.textContent = monthString;

            if (i === 0) {
                option.selected = true;
            }

            periodSelect.appendChild(option);
        }
    }

    // 検索イベント
    const searchBtn = document.getElementById('employee-search-btn');
    const searchInput = document.getElementById('employee-search-input');
    if (searchBtn && searchInput) {
        searchBtn.addEventListener('click', async () => {
            const keyword = searchInput.value.trim();
            try {
                const res = await fetch(`/api/employees?keyword=${encodeURIComponent(keyword)}`);
                if (!res.ok) throw new Error('検索失敗');
                const employees = await res.json();
                
                // 検索結果に対してもアクティブフィルタリングを適用
                const activeEmployees = employees.filter(emp => emp.active === true);
                renderEmployeeRows(activeEmployees);
            } catch (err) {
                alert('検索に失敗しました');
            }
        });
        searchInput.addEventListener('keydown', e => {
            if (e.key === 'Enter') searchBtn.click();
        });
    }

    // 集計実行ボタンのイベントリスナー (HTMLに要素が存在しない場合、このブロックはスキップされる)
    const executeAggregationBtn = document.getElementById('execute-aggregation-btn');

    if (executeAggregationBtn && periodSelect) {
        executeAggregationBtn.addEventListener('click', () => {
            const selectedPeriod = periodSelect.value;
            window.location.href = `payroll_calculation.html?period=${encodeURIComponent(selectedPeriod)}`;
        });
    }

    // 従業員登録モーダル表示・非表示
    const openBtn = document.getElementById('open-register-modal');
    const closeBtn = document.getElementById('close-register-modal');
    const modal = document.getElementById('register-modal');

    if(openBtn && modal && closeBtn) {
        openBtn.addEventListener('click', (e) => {
            // ページ遷移を阻止し、モーダルを表示
            e.preventDefault();
            modal.style.display = 'block';
        });
        closeBtn.addEventListener('click', () => {
            modal.style.display = 'none';
        });
        window.addEventListener('click', (e) => {
            if (e.target === modal) modal.style.display = 'none';
        });
    }
});

// 従業員登録フォーム送信
const form = document.getElementById('employee-register-form');
if(form) {
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        // 登録用データ (簡易版)
        const data = {
            employeeId: form.employeeId.value,
            name: form.employeeName.value,
            department: form.department.value,
            password: form.password.value
            // 実際には roleId, email などのデータも必要
        };
        
        try {
            const operatorId = getLoggedInEmployeeId();
            
            const res = await fetch('/api/employees', {
                method: 'POST',
                headers: { 
                    'Content-Type': 'application/json',
                    'X-Operator-Id': operatorId
                },
                body: JSON.stringify(data)
            });
            
            if (res.ok) {
                alert('登録しました');
                form.reset(); // フォームをリセット
                modal.style.display = 'none'; // モーダルを非表示
                await fetchAndRenderEmployees(); 
            } else {
                const err = await res.json();
                alert('登録失敗: ' + (err.message || 'エラー'));
            }
        } catch (err) {
            alert('通信エラー: ' + err.message);
        }
    });
}