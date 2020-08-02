package jp.co.internous.ocean.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jp.co.internous.ocean.model.domain.MstDestination;

import jp.co.internous.ocean.model.domain.dto.CartDto;
import jp.co.internous.ocean.model.domain.dto.PurchaseHistoryDto;

import com.google.gson.Gson;


import jp.co.internous.ocean.model.mapper.MstDestinationMapper;
import jp.co.internous.ocean.model.mapper.MstProductMapper;
import jp.co.internous.ocean.model.mapper.TblCartMapper;
import jp.co.internous.ocean.model.mapper.TblPurchaseHistoryMapper;
import jp.co.internous.ocean.model.session.LoginSession;

@Controller
@RequestMapping("/ocean/settlement")
public class SettlementController {
	
	@Autowired
	protected LoginSession loginSession;
	@Autowired
	MstDestinationMapper mstDestinationMapper;
	@Autowired
	TblCartMapper tblCartMapper;
	@Autowired
	TblPurchaseHistoryMapper tblPurchaseHistoryMapper;
	@Autowired
	MstProductMapper mstProductMapper;
	@Autowired
	Gson gson = new Gson();
	
	@RequestMapping("/")           //settlement.htmlを表示する
	public String settlement(Model model) {
		model.addAttribute("loginSession", loginSession); 
		int userId = loginSession.getUserId(); //userid 取得
		
		List<MstDestination> destination = mstDestinationMapper.findByUserId(userId);// 宛先一覧をuserIdで取得
		
		model.addAttribute("destination",destination); //宛先をhtmlで参照できるようにする
		
		return "settlement";
	}
	
	@SuppressWarnings("unchecked")
	@ResponseBody
	@RequestMapping("/complete")  //決済ボタンが押された時の動作
	public boolean complete(@RequestBody String destinationId) {
		Map<String, String> map = gson.fromJson(destinationId, Map.class);
		String idd = map.get("destinationId");
		int id = Integer.parseInt(idd);
		int userId = loginSession.getUserId(); //userid 取得
		List<CartDto> cart = tblCartMapper.findByUserId(userId); //cartの情報取得(List)
		
		//PurchaseHistoryに格納 カートサイズの回数繰り返し->毎回insertする
		int insertCount = 0;  //格納数の確認用
		int cartSize = cart.size(); //カートサイズの記録
		for(int i = 0;  i < cart.size(); i++) {  //0からのため "=" なし cartListの数だけ繰り返す
			PurchaseHistoryDto p = new PurchaseHistoryDto();//取り出したものを一時保存するため。毎回newで良い
			
			p.setUserId(userId);  //userid ログインセッションから
			p.setProductId(cart.get(i).getProductId()); //produtid
			p.setProductCount(cart.get(i).getProductCount()); //count
			p.setPrice(cart.get(i).getPrice()); //price
			p.setDestinationId(id); //宛先id htmlから受け取ったもの
			insertCount += tblPurchaseHistoryMapper.insert(p); // insertする
		}
		
		//カート情報の削除
		int result = 0;
		if(insertCount == cart.size()) {
		result = tblCartMapper.deleteByTmpUserId(userId); //userIdに紐付くカート情報の削除
		}
		
		return result == cartSize;// 購入履歴に遷移
	}
}